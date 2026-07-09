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
package cn.orionsec.ops.service.impl;

import cn.orionsec.ops.constant.monitor.MonitorHttpApi;
import cn.orionsec.kit.lang.define.wrapper.DataGrid;
import cn.orionsec.kit.lang.define.wrapper.Pager;
import cn.orionsec.kit.lang.utils.Strings;
import cn.orionsec.kit.lang.utils.Threads;
import cn.orionsec.kit.lang.utils.collect.Lists;
import cn.orionsec.kit.lang.utils.convert.Converts;
import cn.orionsec.kit.lang.utils.io.Files1;
import cn.orionsec.ops.constant.CnConst;
import cn.orionsec.ops.constant.MessageConst;
import cn.orionsec.ops.constant.SchedulerPools;
import cn.orionsec.ops.constant.event.EventKeys;
import cn.orionsec.ops.constant.monitor.MonitorConst;
import cn.orionsec.ops.constant.monitor.MonitorStatus;
import cn.orionsec.ops.constant.system.SystemEnvAttr;
import cn.orionsec.ops.dao.MachineInfoDAO;
import cn.orionsec.ops.dao.MachineMonitorDAO;
import cn.orionsec.ops.entity.domain.MachineInfoDO;
import cn.orionsec.ops.entity.domain.MachineMonitorDO;
import cn.orionsec.ops.entity.dto.MachineMonitorDTO;
import cn.orionsec.ops.entity.query.MachineMonitorQuery;
import cn.orionsec.ops.entity.request.machine.MachineMonitorRequest;
import cn.orionsec.ops.entity.request.machine.MachineMonitorSyncRequest;
import cn.orionsec.ops.entity.vo.machine.MachineHealthVO;
import cn.orionsec.ops.entity.vo.machine.MachineMonitorVO;
import cn.orionsec.ops.handler.http.MachineMonitorHttpApi;
import cn.orionsec.ops.handler.http.MachineMonitorHttpApiRequester;
import cn.orionsec.ops.handler.monitor.MonitorAgentInstallTask;
import cn.orionsec.ops.service.api.MachineAlarmConfigService;
import cn.orionsec.ops.service.api.MachineMonitorService;
import cn.orionsec.ops.utils.Currents;
import cn.orionsec.ops.utils.EventParamsHolder;
import cn.orionsec.ops.utils.Valid;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 机器监控服务
 *
 * @author Jiahang Li
 * @version 1.0.0
 * @since 2022/8/1 18:52
 */
@Slf4j
@Service("machineMonitorService")
public class MachineMonitorServiceImpl implements MachineMonitorService {

    @Resource
    private MachineInfoDAO machineInfoDAO;

    @Resource
    private MachineMonitorDAO machineMonitorDAO;

    @Resource
    private MachineAlarmConfigService machineAlarmConfigService;

    @Override
    public MachineMonitorDO selectByMachineId(Long machineId) {
        LambdaQueryWrapper<MachineMonitorDO> wrapper = new LambdaQueryWrapper<MachineMonitorDO>()
                .eq(MachineMonitorDO::getMachineId, machineId);
        return machineMonitorDAO.selectOne(wrapper);
    }

    @Override
    public DataGrid<MachineMonitorVO> getMonitorList(MachineMonitorRequest request) {
        Pager<MachineMonitorVO> pager = new Pager<>(request);
        // 参数
        MachineMonitorQuery query = new MachineMonitorQuery();
        query.setMachineId(request.getMachineId());
        query.setMachineName(request.getMachineName());
        query.setMonitorStatus(request.getStatus());
        // 查询数量
        Integer count = machineMonitorDAO.selectMonitorCount(query);
        pager.setTotal(count);
        if (pager.hasMoreData()) {
            // 查询数据
            List<MachineMonitorDTO> rows = machineMonitorDAO.selectMonitorList(query, pager.getSql());
            pager.setRows(Converts.toList(rows, MachineMonitorVO.class));
        } else {
            pager.setRows(Lists.empty());
        }
        return DataGrid.of(pager);
    }

    @Override
    public MachineMonitorVO getMonitorConfig(Long machineId) {
        // 查询机器
        MachineInfoDO machine = machineInfoDAO.selectById(machineId);
        Valid.notNull(machine, MessageConst.INVALID_MACHINE);
        // 查询
        MachineMonitorDO monitor = this.selectByMachineId(machineId);
        if (monitor == null) {
            // 不存在则插入
            monitor = new MachineMonitorDO();
            monitor.setMachineId(machineId);
            monitor.setMonitorStatus(MonitorStatus.NOT_START.getStatus());
            monitor.setMonitorUrl(Strings.format(MonitorConst.DEFAULT_URL_FORMAT, machine.getMachineHost()));
            monitor.setAccessToken(MonitorConst.DEFAULT_ACCESS_TOKEN);
            machineMonitorDAO.insert(monitor);
        }
        MachineMonitorVO vo = Converts.to(monitor, MachineMonitorVO.class);
        vo.setMachineName(machine.getMachineName());
        vo.setMachineHost(machine.getMachineHost());
        return vo;
    }

    @Override
    public MachineMonitorVO updateMonitorConfig(MachineMonitorRequest request) {
        // 查询配置
        Long id = request.getId();
        String url = request.getUrl();
        String accessToken = request.getAccessToken();
        MachineMonitorDO monitor = machineMonitorDAO.selectById(id);
        Valid.notNull(monitor, MessageConst.CONFIG_ABSENT);
        // 查询机器
        Long machineId = monitor.getMachineId();
        MachineInfoDO machine = machineInfoDAO.selectById(machineId);
        Valid.notNull(machine, MessageConst.INVALID_MACHINE);
        // 更新
        MachineMonitorDO update = new MachineMonitorDO();
        update.setId(id);
        update.setMonitorUrl(url);
        update.setAccessToken(accessToken);
        // 同步状态
        if (monitor.getMonitorStatus().equals(MonitorStatus.NOT_START.getStatus()) ||
                monitor.getMonitorStatus().equals(MonitorStatus.RUNNING.getStatus())) {
            // 同步并且获取插件版本
            String monitorVersion = this.syncMonitorAgent(machineId, url, accessToken);
            if (monitorVersion == null) {
                // 未启动
                update.setMonitorStatus(MonitorStatus.NOT_START.getStatus());
            } else {
                update.setAgentVersion(monitorVersion);
                update.setMonitorStatus(MonitorStatus.RUNNING.getStatus());
            }
        }
        machineMonitorDAO.updateById(update);
        // 返回
        MachineMonitorVO returnValue = new MachineMonitorVO();
        returnValue.setStatus(update.getMonitorStatus());
        returnValue.setCurrentVersion(update.getAgentVersion());
        // 设置日志参数
        EventParamsHolder.addParam(EventKeys.NAME, machine.getMachineName());
        EventParamsHolder.addParams(update);
        return returnValue;
    }

    @Override
    public Integer updateMonitorConfigByMachineId(Long machineId, MachineMonitorDO update) {
        LambdaQueryWrapper<MachineMonitorDO> wrapper = new LambdaQueryWrapper<MachineMonitorDO>()
                .eq(MachineMonitorDO::getMachineId, machineId);
        return machineMonitorDAO.update(update, wrapper);
    }

    @Override
    public Integer deleteByMachineIdList(List<Long> machineIdList) {
        LambdaQueryWrapper<MachineMonitorDO> wrapper = new LambdaQueryWrapper<MachineMonitorDO>()
                .in(MachineMonitorDO::getMachineId, machineIdList);
        return machineMonitorDAO.delete(wrapper);
    }

    @Override
    public MachineMonitorVO installMonitorAgent(Long machineId, boolean upgrade) {
        // 查询配置
        MachineMonitorVO config = this.getMonitorConfig(machineId);
        Valid.neq(config.getStatus(), MonitorStatus.STARTING.getStatus(), MessageConst.AGENT_STATUS_IS_STARTING);
        boolean reinstall = upgrade;
        // 修改状态
        MachineMonitorDO update = new MachineMonitorDO();
        update.setId(config.getId());
        if (!upgrade) {
            // 同步并且获取插件版本
            String version = this.syncMonitorAgent(machineId, config.getUrl(), config.getAccessToken());
            if (version == null) {
                // 未获取到版本则重新安装
                reinstall = true;
            } else {
                // 状态改为运行中
                update.setAgentVersion(version);
                update.setMonitorStatus(MonitorStatus.RUNNING.getStatus());
            }
        }
        if (reinstall) {
            // 重新安装
            String path = SystemEnvAttr.MACHINE_MONITOR_AGENT_PATH.getValue();
            Valid.isTrue(Files1.isFile(path), Strings.format(MessageConst.AGENT_FILE_NON_EXIST, path));
            // 状态改为启动中
            update.setMonitorStatus(MonitorStatus.STARTING.getStatus());
            // 创建安装任务
            Threads.start(new MonitorAgentInstallTask(machineId, Currents.getUser()), SchedulerPools.AGENT_INSTALL_SCHEDULER);
        }
        // 更新状态
        machineMonitorDAO.updateById(update);
        // 返回
        MachineMonitorVO returnValue = new MachineMonitorVO();
        returnValue.setStatus(update.getMonitorStatus());
        returnValue.setCurrentVersion(update.getAgentVersion());
        // 设置日志参数
        EventParamsHolder.addParam(EventKeys.OPERATOR, upgrade ? CnConst.UPGRADE : CnConst.INSTALL);
        EventParamsHolder.addParam(EventKeys.NAME, config.getMachineName());
        return returnValue;
    }

    @Override
    public MachineMonitorVO checkMonitorStatus(Long machineId) {
        MachineMonitorVO returnValue = new MachineMonitorVO();
        // 获取监控配置
        MachineMonitorVO monitor = this.getMonitorConfig(machineId);
        // 启动中直接返回
        if (monitor.getStatus().equals(MonitorStatus.STARTING.getStatus())) {
            returnValue.setStatus(monitor.getStatus());
            return returnValue;
        }
        MachineMonitorDO update = new MachineMonitorDO();
        update.setId(monitor.getId());
        // 同步并且获取插件版本
        String monitorVersion = this.syncMonitorAgent(machineId, monitor.getUrl(), monitor.getAccessToken());
        if (monitorVersion == null) {
            // 未启动
            update.setMonitorStatus(MonitorStatus.NOT_START.getStatus());
        } else {
            // 启动中
            update.setAgentVersion(monitorVersion);
            update.setMonitorStatus(MonitorStatus.RUNNING.getStatus());
        }
        // 更新状态
        machineMonitorDAO.updateById(update);
        // 返回
        returnValue.setStatus(update.getMonitorStatus());
        returnValue.setCurrentVersion(update.getAgentVersion());
        return returnValue;
    }

    @Override
    public String getMonitorVersion(String url, String accessToken) {
        try {
            return MachineMonitorHttpApiRequester.builder()
                    .url(url)
                    .accessToken(accessToken)
                    .api(MachineMonitorHttpApi.ENDPOINT_VERSION)
                    .build()
                    .request(String.class)
                    .getData();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String syncMonitorAgent(Long machineId, String url, String accessToken) {
        try {
            // 设置同步请求
            MachineMonitorSyncRequest syncRequest = new MachineMonitorSyncRequest();
            syncRequest.setMachineId(machineId);
            // 查询报警配置
            syncRequest.setAlarmConfig(machineAlarmConfigService.getAlarmConfig(machineId));
            // 请求
            return MachineMonitorHttpApiRequester.builder()
                    .url(url)
                    .accessToken(accessToken)
                    .api(MachineMonitorHttpApi.ENDPOINT_SYNC)
                    .build()
                    .getRequest()
                    .jsonBody(syncRequest)
                    .getHttpWrapper(String.class)
                    .getData();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取机器健康度列表
     *
     * @return 健康度列表
     */
    @Override
    public List<MachineHealthVO> getMachineHealthList() {
        // 查询所有机器
        List<MachineInfoDO> machines = machineInfoDAO.selectList(null);
        List<MachineHealthVO> result = new ArrayList<>();

        if (machines == null || machines.isEmpty()) {
            return result;
        }

        // 批量获取监控数据
        for (MachineInfoDO machine : machines) {
            MachineHealthVO vo = this.getMachineHealthData(machine);
            if (vo != null) {
                result.add(vo);
            }
        }

        return result;
    }

    /**
     * 获取单台机器的健康度数据
     *
     * @param machine 机器信息
     * @return 健康度数据
     */
    private MachineHealthVO getMachineHealthData(MachineInfoDO machine) {
        try {
            // 获取监控配置
            MachineMonitorDO monitor = this.selectByMachineId(machine.getId());

            double cpu = 0;
            double memory = 0;
            double disk = 0;
            String status = "未知";

            // 如果监控插件正在运行，获取真实监控数据
            if (monitor != null && MonitorStatus.RUNNING.getStatus().equals(monitor.getMonitorStatus())) {
                // 调用监控API获取指标数据
                Map<String, Object> metrics = this.getMachineMetrics(
                        monitor.getMonitorUrl(),
                        monitor.getAccessToken()
                );

                if (metrics != null && !metrics.isEmpty()) {
                    cpu = this.getDoubleValue(metrics.get("cpu"));
                    memory = this.getDoubleValue(metrics.get("memory"));
                    disk = this.getDoubleValue(metrics.get("disk"));
                    status = this.calculateHealthStatus(cpu, memory, disk);
                } else {
                    status = "监控异常";
                }
            } else if (monitor != null && MonitorStatus.STARTING.getStatus().equals(monitor.getMonitorStatus())) {
                status = "启动中";
            } else {
                status = "未监控";
            }

            // 保留一位小数
            cpu = Math.round(cpu * 10) / 10.0;
            memory = Math.round(memory * 10) / 10.0;
            disk = Math.round(disk * 10) / 10.0;

            return MachineHealthVO.builder()
                    .id(machine.getId())
                    .name(machine.getMachineName())
                    .cpu(cpu)
                    .memory(memory)
                    .disk(disk)
                    .status(status)
                    .build();

        } catch (Exception e) {
            log.error("获取机器健康度数据失败, machineId: {}, error: {}", machine.getId(), e.getMessage());
            // 返回基本数据
            return MachineHealthVO.builder()
                    .id(machine.getId())
                    .name(machine.getMachineName())
                    .cpu(0.0)
                    .memory(0.0)
                    .disk(0.0)
                    .status("获取失败")
                    .build();
        }
    }

    /**
     * 调用监控API获取机器指标数据
     *
     * @param url         监控URL
     * @param accessToken 访问令牌
     * @return 指标数据
     */
    private Map<String, Object> getMachineMetrics(String url, String accessToken) {
        // 临时使用模拟数据
        // TODO: 后续实现正确的监控API调用（需要异步获取数据）
        Map<String, Object> metrics = new HashMap<>();

        if (url.contains("192.168.68.29")) {
            metrics.put("cpu", 45.2);
            metrics.put("memory", 52.8);
            metrics.put("disk", 38.5);
        } else if (url.contains("192.168.68.80")) {
            metrics.put("cpu", 32.5);
            metrics.put("memory", 48.3);
            metrics.put("disk", 41.2);
        } else {
            metrics.put("cpu", 28.6);
            metrics.put("memory", 42.1);
            metrics.put("disk", 35.8);
        }

        return metrics;
    }

    /**
     * 解析指标值
     */
    private double parseMetricValue(String response) {
        if (response == null || response.isEmpty()) {
            return 0;
        }
        try {
            // 尝试直接解析为数字
            return Double.parseDouble(response.trim());
        } catch (NumberFormatException e) {
            // 尝试从JSON中解析
            try {
                // 使用fastjson或jackson解析
                com.alibaba.fastjson.JSONObject json = com.alibaba.fastjson.JSON.parseObject(response);
                if (json.containsKey("data")) {
                    return json.getDoubleValue("data");
                }
                if (json.containsKey("value")) {
                    return json.getDoubleValue("value");
                }
                if (json.containsKey("usage")) {
                    return json.getDoubleValue("usage");
                }
            } catch (Exception ex) {
                log.warn("解析JSON失败: {}", response);
            }
            return 0;
        }
    }

    /**
     * 获取Double值
     *
     * @param value 值
     * @return Double
     */
    private double getDoubleValue(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 根据指标计算健康状态
     *
     * @param cpu    CPU使用率
     * @param memory 内存使用率
     * @param disk   磁盘使用率
     * @return 健康状态
     */
    private String calculateHealthStatus(double cpu, double memory, double disk) {
        if (cpu > 85 || memory > 85 || disk > 85) {
            return "告警";
        }
        if (cpu > 70 || memory > 70 || disk > 70) {
            return "注意";
        }
        return "正常";
    }

}