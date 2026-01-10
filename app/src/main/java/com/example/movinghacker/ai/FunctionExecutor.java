package com.example.movinghacker.ai;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Function执行器
 * 负责异步执行Function调用
 */
public class FunctionExecutor {
    private static final String TAG = "FunctionExecutor";
    private ExecutorService executorService;
    private FunctionRegistry registry;

    public FunctionExecutor() {
        this.executorService = Executors.newCachedThreadPool();
        this.registry = FunctionRegistry.getInstance();
    }

    /**
     * 异步执行单个Function
     * @param call Function调用
     * @param callback 回调接口
     */
    public void executeAsync(FunctionCall call, ExecutionCallback callback) {
        executorService.execute(() -> {
            try {
                FunctionResult result = registry.execute(call);
                if (callback != null) {
                    callback.onSuccess(result);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error executing function: " + call.getName(), e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }

    /**
     * 同步执行单个Function
     * @param call Function调用
     * @return 执行结果
     */
    public FunctionResult executeSync(FunctionCall call) {
        return registry.execute(call);
    }

    /**
     * 批量执行多个Function
     * @param calls Function调用列表
     * @return 执行结果列表
     */
    public List<FunctionResult> executeBatch(List<FunctionCall> calls) {
        List<FunctionResult> results = new ArrayList<>();
        
        if (calls == null || calls.isEmpty()) {
            return results;
        }

        for (FunctionCall call : calls) {
            try {
                FunctionResult result = registry.execute(call);
                results.add(result);
            } catch (Exception e) {
                Log.e(TAG, "Error in batch execution: " + call.getName(), e);
                results.add(FunctionResult.error(call.getId(), e.getMessage()));
            }
        }

        return results;
    }

    /**
     * 异步批量执行多个Function
     * @param calls Function调用列表
     * @param callback 回调接口
     */
    public void executeBatchAsync(List<FunctionCall> calls, BatchExecutionCallback callback) {
        executorService.execute(() -> {
            try {
                List<FunctionResult> results = executeBatch(calls);
                if (callback != null) {
                    callback.onSuccess(results);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in batch execution", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }

    /**
     * 关闭执行器
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    /**
     * 单个Function执行回调接口
     */
    public interface ExecutionCallback {
        void onSuccess(FunctionResult result);
        void onError(Exception e);
    }

    /**
     * 批量Function执行回调接口
     */
    public interface BatchExecutionCallback {
        void onSuccess(List<FunctionResult> results);
        void onError(Exception e);
    }
}
