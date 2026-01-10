package com.example.movinghacker.ai;

/**
 * Function执行结果数据模型
 */
public class FunctionResult {
    private String functionCallId;
    private String result;  // JSON格式的结果
    private boolean success;
    private String error;

    public FunctionResult() {
    }

    public FunctionResult(String functionCallId, String result, boolean success, String error) {
        this.functionCallId = functionCallId;
        this.result = result;
        this.success = success;
        this.error = error;
    }

    public static FunctionResult success(String functionCallId, String result) {
        return new FunctionResult(functionCallId, result, true, null);
    }

    public static FunctionResult error(String functionCallId, String error) {
        return new FunctionResult(functionCallId, null, false, error);
    }

    // Getters and Setters
    public String getFunctionCallId() {
        return functionCallId;
    }

    public void setFunctionCallId(String functionCallId) {
        this.functionCallId = functionCallId;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "FunctionResult{" +
                "functionCallId='" + functionCallId + '\'' +
                ", success=" + success +
                ", result='" + result + '\'' +
                ", error='" + error + '\'' +
                '}';
    }
}
