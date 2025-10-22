package com.inductiveautomation.ignition.examples.python3.gateway;

/**
 * Result of a Python 3 execution
 */
public class Python3Result {

    private final boolean success;
    private final Object result;
    private final String error;
    private final String traceback;

    public Python3Result(boolean success, Object result, String error, String traceback) {
        this.success = success;
        this.result = result;
        this.error = error;
        this.traceback = traceback;
    }

    public boolean isSuccess() {
        return success;
    }

    public Object getResult() {
        return result;
    }

    public String getError() {
        return error;
    }

    public String getTraceback() {
        return traceback;
    }

    /**
     * Get result or throw exception if failed
     */
    public Object getResultOrThrow() throws Python3Exception {
        if (!success) {
            throw new Python3Exception(error, traceback);
        }
        return result;
    }

    @Override
    public String toString() {
        if (success) {
            return "Python3Result{success=true, result=" + result + "}";
        } else {
            return "Python3Result{success=false, error=" + error + "}";
        }
    }
}
