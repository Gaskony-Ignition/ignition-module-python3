package com.gaskony.python3.gateway;

import com.inductiveautomation.ignition.gateway.dataroutes.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Resolves the actual Ignition roles attached to an authenticated request.
 *
 * <p>Used by {@code ExecutionHandlers#handleCreateSession} (C14 fix) to bind the
 * security mode of an issued session token to the caller's <em>real</em> Ignition
 * role membership rather than to a self-asserted {@code client_id} field.
 *
 * <p>Implementation strategy:
 * <ol>
 *   <li>Primary path uses the Ignition 8.3 {@code WebUiSession} SDK API
 *       ({@code com.inductiveautomation.ignition.gateway.web.session.WebUiSession})
 *       via reflection so that:
 *       <ul>
 *         <li>this class still compiles when the SDK adds/changes the API surface,
 *         <li>unit tests can run against a stub without dragging in the full
 *             Gateway-API jar.
 *       </ul>
 *   <li>Returns {@link Optional#empty()} when the request is unauthenticated,
 *       when the SDK class is unavailable, or when no roles can be derived.
 * </ol>
 *
 * <p>The class is package-private and stateless. A default instance is exposed
 * via {@link #getDefault()}; tests substitute their own instance via the
 * {@link ExecutionHandlers#setRoleResolverForTesting(RoleResolver)} hook.
 *
 * @since v3.13.0 (C14 — bind /auth/session token issuance to actual role)
 */
class RoleResolver {

    private static final Logger logger = LoggerFactory.getLogger(RoleResolver.class);

    private static final RoleResolver DEFAULT = new RoleResolver();

    /** Canonical Ignition role name for full administrative access. */
    static final String ADMIN_ROLE = "Administrator";

    /** Canonical Ignition role name commonly granted to Designer-class users. */
    static final String DESIGNER_ROLE = "Designer";

    static RoleResolver getDefault() {
        return DEFAULT;
    }

    /**
     * Resolve the role names attached to the authenticated user behind {@code req}.
     *
     * @param req the request context (may be {@code null})
     * @return immutable, lower-case-normalised set of role names; never {@code null}.
     *         Empty when the request is unauthenticated or roles cannot be resolved.
     */
    Set<String> getRoles(RequestContext req) {
        if (req == null) {
            return Collections.emptySet();
        }

        // Attempt the WebUiSession reflection chain. Any failure → empty set.
        try {
            Class<?> webUiSessionCls;
            try {
                webUiSessionCls = Class.forName(
                    "com.inductiveautomation.ignition.gateway.web.session.WebUiSession");
            } catch (ClassNotFoundException missing) {
                logger.debug("WebUiSession SDK class not on classpath (likely a unit-test environment); "
                    + "no roles available via reflection");
                return Collections.emptySet();
            }

            Method findMethod = webUiSessionCls.getMethod("find", RequestContext.class);
            Object sessionOpt = findMethod.invoke(null, req);
            if (!(sessionOpt instanceof Optional<?>)) {
                return Collections.emptySet();
            }
            Optional<?> session = (Optional<?>) sessionOpt;
            if (session.isEmpty()) {
                return Collections.emptySet();
            }

            Object sessionObj = session.get();
            Object userCtx = sessionObj.getClass().getMethod("getUserContext").invoke(sessionObj);
            if (userCtx == null) {
                return Collections.emptySet();
            }

            Object webAuthUserOpt = userCtx.getClass().getMethod("getWebAuthUser").invoke(userCtx);
            if (!(webAuthUserOpt instanceof Optional<?>)) {
                return Collections.emptySet();
            }
            Optional<?> webAuthUser = (Optional<?>) webAuthUserOpt;
            if (webAuthUser.isEmpty()) {
                return Collections.emptySet();
            }

            Object user = webAuthUser.get();
            Object rolesObj = user.getClass().getMethod("getRoles").invoke(user);
            if (!(rolesObj instanceof Collection<?>)) {
                return Collections.emptySet();
            }

            Set<String> normalised = new LinkedHashSet<>();
            for (Object r : (Collection<?>) rolesObj) {
                if (r != null) {
                    normalised.add(r.toString());
                }
            }
            return Collections.unmodifiableSet(normalised);
        } catch (Exception e) {
            logger.debug("Failed to resolve roles via WebUiSession: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    /**
     * @return {@code true} iff the caller has the Ignition {@value #ADMIN_ROLE} role.
     */
    boolean isAdministrator(RequestContext req) {
        Set<String> roles = getRoles(req);
        for (String r : roles) {
            if (ADMIN_ROLE.equalsIgnoreCase(r)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return {@code true} iff the caller has the Ignition {@value #DESIGNER_ROLE} role
     *         <em>or</em> the {@value #ADMIN_ROLE} role.
     */
    boolean isDesignerOrAdministrator(RequestContext req) {
        Set<String> roles = getRoles(req);
        for (String r : roles) {
            if (ADMIN_ROLE.equalsIgnoreCase(r) || DESIGNER_ROLE.equalsIgnoreCase(r)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Require the caller of {@code req} to have the Ignition {@value #ADMIN_ROLE}
     * role. Used by REST routes that mint or guard privileged operations.
     *
     * <p>Throws a {@link SecurityException} (rather than returning a boolean) so
     * the caller can let it propagate and the route's standard error wrapper
     * surfaces a 403 with a clear message. Security exceptions intentionally do
     * not include a stack trace in the response (handled by the wrapper).</p>
     *
     * @param req the request context (must not be {@code null})
     * @throws SecurityException if the caller lacks the Administrator role
     * @since v3.13.0 (C13 — Administrator role gate for Python execution)
     */
    void requireAdministrator(RequestContext req) {
        if (!isAdministrator(req)) {
            throw new SecurityException(
                "Administrator role required to execute Python");
        }
    }

    /**
     * Scripting-side role check, used by {@link Python3ScriptModule} to gate
     * {@code system.python3.exec} (and friends) at the Jython entry-point.
     *
     * <p>Scripting functions invoked from Jython do not carry an HTTP
     * {@link RequestContext}. Some invocation paths run with no per-call user
     * identity at all (e.g. a Gateway-scoped tag-change or timer script
     * executes as the Gateway service user). Treating those as "Administrator"
     * by default would leave low-trust project Jython scripts as a
     * privilege-escalation vector into Python 3 execution.</p>
     *
     * <p>The default policy is therefore deny-by-default: the system property
     * {@code ignition.python3.scriptingFunctions.allowed} (or the equivalent
     * {@code IGNITION_PYTHON3_SCRIPTING_ALLOWED} environment variable) must be
     * explicitly set to {@code true} for scripting calls to succeed. This
     * makes the binding opt-in by an Ignition Administrator at install time —
     * in practice an Administrator-only operation — and is the practical
     * equivalent of "require Administrator role" for the no-RequestContext
     * case.</p>
     *
     * <p>Subclasses (notably the test stub) may override
     * {@link #isScriptingCallerAdministrator()} to bypass the system-property
     * gate.</p>
     *
     * @throws SecurityException if scripting access is not enabled
     * @since v3.13.0 (C13)
     */
    void requireAdministratorForScripting() {
        if (!isScriptingCallerAdministrator()) {
            throw new SecurityException(
                "Administrator role required to execute Python via "
                + "system.python3.* scripting functions. Set system property "
                + "'ignition.python3.scriptingFunctions.allowed=true' (or the "
                + "IGNITION_PYTHON3_SCRIPTING_ALLOWED env var) on the Gateway to "
                + "explicitly opt-in to scripting access for trusted projects.");
        }
    }

    /**
     * Hook for {@link #requireAdministratorForScripting()}. Default impl reads
     * the system property / env var; tests substitute their own resolver via
     * {@link Python3ScriptModule#setRoleResolverForTesting(RoleResolver)}.
     */
    boolean isScriptingCallerAdministrator() {
        String prop = System.getProperty("ignition.python3.scriptingFunctions.allowed");
        if (prop != null && Boolean.parseBoolean(prop.trim())) {
            return true;
        }
        String env = System.getenv("IGNITION_PYTHON3_SCRIPTING_ALLOWED");
        if (env != null && Boolean.parseBoolean(env.trim())) {
            return true;
        }
        return false;
    }
}
