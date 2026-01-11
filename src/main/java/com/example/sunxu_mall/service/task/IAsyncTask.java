package com.example.sunxu_mall.service.task;

import com.example.sunxu_mall.entity.common.CommonTaskEntity;

/**
 * @author sunxu
 * @version 1.0
 * @date 2026/1/9 21:20
 * @description
 */
public interface IAsyncTask {
    /**
     * do task
     *
     * @param commonTaskEntity commonTaskEntity
     */
    void doTask(CommonTaskEntity commonTaskEntity);
}
