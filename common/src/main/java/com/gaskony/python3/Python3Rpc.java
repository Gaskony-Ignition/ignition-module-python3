package com.gaskony.python3;

import com.inductiveautomation.ignition.common.rpc.RpcInterface;

/**
 * Module RPC interface for authenticated Designer &rarr; Gateway communication.
 *
 * <p>Introduced in v4.2.0 to replace the Designer's cold-HTTP REST client, which
 * could no longer authenticate to the Gateway after the C13/C14 security
 * hardening removed the {@code X-Source} header bypass and the self-asserted
 * {@code client_id} session-token grant. A stand-alone {@code java.net.http}
 * client carries none of the Designer's authenticated Gateway session, so every
 * REST call was rejected (401) and the Project Browser showed
 * "(Gateway unavailable)".</p>
 *
 * <p>Module RPC calls instead travel over the Designer's existing authenticated
 * Gateway channel. The Gateway can therefore resolve the real caller
 * ({@link com.inductiveautomation.ignition.gateway.rpc.RpcDelegate#session()})
 * and no session-token dance is required.</p>
 *
 * <p>Every method returns the <em>same JSON string</em> the equivalent REST
 * endpoint returns, so the Designer-side JSON parsing is unchanged. Method names
 * map to {@code RpcCall.function()} and {@link #packageId} to
 * {@code RpcCall.packageId()}; the module id is supplied by the caller.</p>
 *
 * @since v4.2.0
 */
@RpcInterface(packageId = "python3")
public interface Python3Rpc {

    /** @return JSON {@code {"success":true,"scripts":[...]}} — see REST GET /scripts/list. */
    String listScripts() throws Exception;

    /** @return JSON {@code {"success":true,"script":{...}}} — see REST GET /scripts/load/:name. */
    String loadScript(String name) throws Exception;

    /** @return JSON {@code {"success":true}} — see REST POST /scripts/save. */
    String saveScript(String name, String code, String description,
                      String author, String folderPath, String version) throws Exception;

    /** @return JSON {@code {"success":...,"message":"..."}} — see REST POST /scripts/delete/:name. */
    String deleteScript(String name) throws Exception;

    /**
     * Execute Python statements.
     *
     * @param variablesJson JSON object of variables (may be {@code null}/blank)
     * @param pythonVersion target Python version (may be {@code null}/blank for default)
     * @return JSON {@code {"success":...,"result":...,"error":...}} — see REST POST /exec.
     */
    String exec(String code, String variablesJson, String pythonVersion) throws Exception;

    /**
     * Evaluate a Python expression.
     *
     * @param variablesJson JSON object of variables (may be {@code null}/blank)
     * @param pythonVersion target Python version (may be {@code null}/blank for default)
     * @return JSON {@code {"success":...,"result":...,"error":...}} — see REST POST /eval.
     */
    String eval(String expression, String variablesJson, String pythonVersion) throws Exception;

    /** @return JSON with {@code version}/{@code pythonVersion} fields — see REST GET /version. */
    String getVersion() throws Exception;

    /** @return JSON pool statistics — see REST GET /pool-stats. */
    String getPoolStats() throws Exception;

    /** @return JSON {@code {"healthy":...,"available":...}} — see REST GET /health. */
    String health() throws Exception;
}
