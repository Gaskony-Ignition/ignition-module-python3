package com.inductiveautomation.ignition.examples.python3.designer.testharness;

import com.inductiveautomation.ignition.designer.model.DesignerContext;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Mock implementation of DesignerContext for standalone IDE testing.
 * <p>
 * This mock provides minimal stubs to allow Python3IDE to run outside
 * the Ignition Designer environment for rapid UX development.
 * <p>
 * Uses dynamic proxy to handle all DesignerContext methods gracefully,
 * returning sensible defaults for common types.
 */
public class MockDesignerContext {

    /**
     * Create a mock DesignerContext using dynamic proxy.
     *
     * @return A DesignerContext proxy that returns sensible defaults
     */
    public static DesignerContext create() {
        JFrame mockFrame = new JFrame("Mock Designer Frame");
        mockFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        mockFrame.setSize(1024, 768);

        return (DesignerContext) Proxy.newProxyInstance(
            DesignerContext.class.getClassLoader(),
            new Class<?>[] { DesignerContext.class },
            new MockDesignerInvocationHandler(mockFrame)
        );
    }

    /**
     * Dynamic proxy handler for DesignerContext methods.
     */
    private static class MockDesignerInvocationHandler implements InvocationHandler {
        private final JFrame frame;

        public MockDesignerInvocationHandler(JFrame frame) {
            this.frame = frame;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();

            // Handle specific methods
            switch (methodName) {
                case "getFrame":
                    return frame;
                case "getGatewayAddress":
                    return "http://localhost:9089";
                case "isConnected":
                    return true;
                case "getClientId":
                    return "mock-client-id";
                case "toString":
                    return "MockDesignerContext{frame=" + frame + "}";
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "equals":
                    return proxy == args[0];
            }

            // Return sensible defaults based on return type
            Class<?> returnType = method.getReturnType();
            if (returnType == boolean.class) {
                return false;
            } else if (returnType == int.class) {
                return 0;
            } else if (returnType == long.class) {
                return 0L;
            } else if (returnType == double.class) {
                return 0.0;
            } else if (returnType == String.class) {
                return "";
            } else if (returnType == void.class) {
                return null;
            }

            // For all other methods, return null (objects)
            return null;
        }
    }
}
