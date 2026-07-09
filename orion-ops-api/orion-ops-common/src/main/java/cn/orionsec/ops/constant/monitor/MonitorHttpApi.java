/*
 * Copyright (c) 2021 - present Jiahang Li All rights reserved.
 *
 *   https://ops.orionsec.cn
 *
 * Members:
 *   Jiahang Li - ljh1553488six@139.com - author
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.orionsec.ops.constant.monitor;

/**
 * 监控HTTP接口常量
 *
 * @author Jiahang Li
 * @version 1.0.0
 * @since 2022/8/2
 */
public class MonitorHttpApi {

    /**
     * 获取版本信息接口
     */
    public static final String ENDPOINT_VERSION = "/api/version";

    /**
     * 同步接口
     */
    public static final String ENDPOINT_SYNC = "/api/sync";

    /**
     * 获取指标数据接口
     */
    public static final String ENDPOINT_METRICS = "/api/metrics";

    /**
     * 获取CPU使用率接口
     */
    public static final String ENDPOINT_CPU = "/api/cpu";

    /**
     * 获取内存使用率接口
     */
    public static final String ENDPOINT_MEMORY = "/api/memory";

    /**
     * 获取磁盘使用率接口
     */
    public static final String ENDPOINT_DISK = "/api/disk";

}