<template>
  <div class="summary-container">
    <!-- 第一层 信息&监控 -->
    <a-row class="metrics-floor-1">
      <!-- 信息 -->
      <a-col :span="10">
        <div class="machine-card-wrapper machine-summary-wrapper">
          <a-card :bodyStyle="{padding: '18px', height: '392px'}">
            <template v-if="summaryLoading || !summaryData">
              <a-skeleton active :title="true" :paragraph="{rows: 9}"/>
            </template>
            <template v-else-if="summaryData">
              <h3 class="card-title">机器信息</h3>
              <div class="machine-summary-info-wrapper">
                <span class="machine-info-label">机器id</span>
                <span class="machine-info-value">{{ summaryData.machineId }}</span>
              </div>
              <div class="machine-summary-info-wrapper">
                <span class="machine-info-label">主机名称</span>
                <span class="machine-info-value">{{ summaryData.os.hostname }}</span>
              </div>
              <div class="machine-summary-info-wrapper">
                <span class="machine-info-label">
                  系统负载
                  <a-tooltip v-if="summaryData.isWindows" title="windows 无法统计">
                    <a-icon type="question-circle"/>
                  </a-tooltip>
                </span>
                <span class="machine-info-value">
                  <template v-if="summaryData.isWindows">
                    -
                  </template>
                  <template v-else>
                    {{ load.oneMinuteLoad }}, {{ load.fiveMinuteLoad }}, {{ load.fifteenMinuteLoad }}
                  </template>
                </span>
              </div>
              <div class="machine-summary-info-wrapper">
                <span class="machine-info-label">启动时间</span>
                <span class="machine-info-value">{{ summaryData.os.uptime }}</span>
              </div>
              <div class="machine-summary-info-wrapper">
                <span class="machine-info-label">系统版本</span>
                <span class="machine-info-value" :title="summaryData.os.osName">{{ summaryData.os.osName }}</span>
              </div>
              <div class="machine-summary-info-wrapper">
                <span class="machine-info-label">CPU 型号</span>
                <span class="machine-info-value" :title="summaryData.os.cpuName">{{ summaryData.os.cpuName }}</span>
              </div>
              <div class="machine-summary-info-wrapper">
                <span class="machine-info-label">CPU 核心数</span>
                <span class="machine-info-value">{{ summaryData.os.cpuLogicalCore }}核</span>
              </div>
              <div class="machine-summary-info-wrapper">
                <span class="machine-info-label">总内存</span>
                <span class="machine-info-value">{{ summaryData.os.totalMemory }}</span>
              </div>
              <div class="machine-summary-info-wrapper">
                <span class="machine-info-label">采集状态</span>
                <span class="machine-info-value">
                  <a-badge :status="summaryData.status ? 'processing' : 'default'"/>
                  {{ summaryData.status ? '采集中' : '已暂停' }}
                </span>
              </div>
              <div class="machine-summary-info-wrapper">
                <span class="machine-info-label">插件版本</span>
                <span class="machine-info-value">V{{ summaryData.version }}</span>
              </div>
            </template>
          </a-card>
        </div>
      </a-col>
      <!-- 监控 -->
      <a-col :span="14">
        <a-row>
          <!-- CPU 使用率 -->
          <a-col :span="12">
            <div class="machine-card-wrapper machine-chart-top-wrapper machine-chart-left-wrapper">
              <a-card :loading="chartLoading" :bodyStyle="{padding: '18px 18px 0 18px', height: '186px'}">
                <div class="chart-header-wrapper">
                  <h3 class="card-title">CPU使用率 (%) </h3>
                  <div class="chart-statistic-wrapper">
                    <span class="normal-label chart-statistic-label">当前</span>
                    <span class="chart-statistic-value">{{ charts.cpu.curr }}%</span>
                    <span class="normal-label chart-statistic-label">最大</span>
                    <span class="chart-statistic-value">{{ charts.cpu.max }}%</span>
                  </div>
                </div>
                <div id="cpu-chart" class="chart-container"/>
              </a-card>
            </div>
          </a-col>
          <!-- 网络带宽 -->
          <a-col :span="12">
            <div class="machine-card-wrapper machine-chart-top-wrapper machine-chart-right-wrapper">
              <a-card :loading="chartLoading" :bodyStyle="{padding: '18px', height: '186px'}">
                <div class="chart-header-wrapper">
                  <h3 class="card-title">带宽使用 (Mbps) </h3>
                  <div class="chart-statistic-wrapper">
                    <span class="normal-label chart-statistic-label">发送</span>
                    <span class="chart-statistic-value">{{ charts.net.sent }}</span>
                    <span class="normal-label chart-statistic-label">接收</span>
                    <span class="chart-statistic-value">{{ charts.net.recv }}</span>
                  </div>
                </div>
                <div id="net-chart" class="chart-container"/>
              </a-card>
            </div>
          </a-col>
        </a-row>
        <a-row>
          <!-- 内存使用率 -->
          <a-col :span="12">
            <div class="machine-card-wrapper machine-chart-bottom-wrapper machine-chart-left-wrapper">
              <a-card :loading="chartLoading" :bodyStyle="{padding: '18px 18px 0 18px', height: '186px'}">
                <div class="chart-header-wrapper">
                  <h3 class="card-title">内存使用率 (%) </h3>
                  <div class="chart-statistic-wrapper">
                    <span class="normal-label chart-statistic-label">当前</span>
                    <span class="chart-statistic-value">{{ charts.memory.currUsage }}%</span>
                  </div>
                </div>
                <div id="memory-usage-chart" class="chart-container"/>
              </a-card>
            </div>
          </a-col>
          <!-- 内存使用量 -->
          <a-col :span="12">
            <div class="machine-card-wrapper machine-chart-bottom-wrapper machine-chart-right-wrapper">
              <a-card :loading="chartLoading" :bodyStyle="{padding: '18px 18px 0 18px', height: '186px'}">
                <div class="chart-header-wrapper">
                  <h3 class="card-title">内存使用量 (MB) </h3>
                  <div class="chart-statistic-wrapper">
                    <span class="normal-label chart-statistic-label">当前</span>
                    <span class="chart-statistic-value">{{ charts.memory.currSize }}MB</span>
                  </div>
                </div>
                <div id="memory-size-chart" class="chart-container"/>
              </a-card>
            </div>
          </a-col>
        </a-row>
      </a-col>
    </a-row>
    <!-- 第二层 硬盘&进程 -->
    <a-row class="metrics-floor-2">
      <!-- 硬盘 -->
      <a-col :span="6">
        <div class="machine-card-wrapper machine-disk-wrapper">
          <a-card :loading="summaryLoading" :bodyStyle="{padding: '18px'}">
            <h3 class="card-title">硬盘信息</h3>
            <div class="disk-info-container">
              <div class="disk-info-box" v-for="(disk, index) of disks" :key="index">
                <div class="disk-info-wrapper">
                  <!-- 硬盘信息 -->
                  <div class="disk-text-wrapper">
                    <div class="disk-name">{{ formatDiskName(disk.name) }}</div>
                    <div class="disk-detail">
                      <div class="disk-item">
                        <span class="disk-label">可用</span>
                        <span class="disk-value">{{ disk.freeSpace || '-' }}</span>
                      </div>
                      <div class="disk-item">
                        <span class="disk-label">已用</span>
                        <span class="disk-value">{{ disk.usageSpace || '-' }}</span>
                      </div>
                      <div class="disk-item">
                        <span class="disk-label">全部</span>
                        <span class="disk-value">{{ disk.totalSpace || '-' }}</span>
                      </div>
                    </div>
                  </div>
                  <!-- 使用进度 -->
                  <div class="disk-using-progress">
                    <a-progress type="circle" :width="100" :percent="disk.usage || 0">
                      <template #format="percent">
                        <span class="disk-using-rate-text">{{ percent }}%</span>
                      </template>
                    </a-progress>
                  </div>
                </div>
                <!-- 分割线 -->
                <a-divider v-if="index !== disks.length - 1" style="margin: 16px 0"/>
              </div>
            </div>
          </a-card>
        </div>
      </a-col>
      <!-- 进程 -->
      <a-col :span="18">
        <div class="machine-card-wrapper machine-processes-wrapper">
          <a-card :loading="summaryLoading" :bodyStyle="{padding: '18px'}">
            <div class="processes-container">
              <!-- 进程标题 -->
              <div class="processes-title-wrapper">
                <h3 class="card-title">进程 Top 10</h3>
                <div class="processes-search-wrapper">
                  <a-input class="processes-search-input"
                           v-model="searchProcessName"
                           placeholder="命令"/>
                  <a-icon class="processes-search-icon" type="search" title="搜索" @click="reloadProcess(false)"/>
                  <a-icon class="processes-search-icon" type="reload" title="重置" @click="reloadProcess(true)"/>
                </div>
              </div>
              <!-- 进程表格 -->
              <a-table :columns="columns"
                       :dataSource="processes"
                       :pagination="false"
                       :loading="processLoading"
                       rowKey="pid"
                       size="middle">
                <!-- cpu -->
                <template #cpu="record">
                  {{ record.cpuUsage }}%
                </template>
                <!-- command -->
                <template #command="record">
                  {{ record.name }}
                  <a-tooltip title="查看命令详情">
                    <a-icon class="pointer" type="eye" @click="preview(record.commandLine)"/>
                  </a-tooltip>
                </template>
              </a-table>
            </div>
          </a-card>
        </div>
      </a-col>
    </a-row>
    <!-- 预览框 -->
    <TextPreview ref="preview" :width="700"/>
  </div>
</template>

<script>
import { Chart } from '@antv/g2'
import { formatSecond } from '@/lib/utils'
import { timestampRender } from '@/lib/chart'
import TextPreview from '@/components/preview/TextPreview'

const columns = [
  {
    title: 'PID',
    dataIndex: 'pid',
    key: 'pid',
    width: 100,
    ellipsis: true
  },
  {
    title: 'USER',
    key: 'user',
    dataIndex: 'user',
    width: 120,
    ellipsis: true
  },
  {
    title: 'CPU',
    key: 'cpuUsage',
    width: 120,
    ellipsis: true,
    scopedSlots: { customRender: 'cpu' }
  },
  {
    title: 'MEM',
    key: 'memoryUsage',
    dataIndex: 'memoryUsage',
    width: 120,
    ellipsis: true
  },
  {
    title: 'UP',
    key: 'uptime',
    dataIndex: 'uptime',
    width: 160,
    ellipsis: true
  },
  {
    title: 'COMMAND',
    key: 'command',
    width: 160,
    scopedSlots: { customRender: 'command' }
  }
]

export default {
  name: 'MachineMonitorSummary',
  components: { TextPreview },
  props: {
    machineId: Number,
    config: {
      type: Object,
      default: () => {
        return {}
      }
    }
  },
  data() {
    return {
      summaryLoading: false,
      chartLoading: false,
      processLoading: false,
      summaryData: {},
      load: {},
      disks: [],
      processes: [],
      charts: {
        cpu: {
          chart: null,
          max: null,
          curr: null
        },
        memory: {
          usageChart: null,
          sizeChart: null,
          currUsage: null,
          currSize: null
        },
        net: {
          chart: null,
          sent: null,
          recv: null
        }
      },
      searchProcessName: undefined,
      columns
    }
  },
  methods: {
    // 格式化硬盘名称，处理特殊字符
    formatDiskName(name) {
      if (!name) return '-'
      // 清理特殊字符，只保留可读字符
      let cleanName = name.replace(/[^\w\s\-:]/g, '')
      // 如果是 Windows 盘符格式，显示为 "C: 盘"
      if (cleanName.match(/^[A-Za-z]:/)) {
        return cleanName
      }
      return cleanName || '硬盘'
    },
    doRefresh() {
      if (!this.summaryData) {
        return
      }
      if (!this.summaryData.isWindows) {
        this.reloadSystemLoad()
      }
      this.reloadProcess(false, false)
      this.loadChart(false)
    },
    reloadProcess(clear, loading = true) {
      this.processLoading = loading
      if (clear) {
        this.searchProcessName = undefined
      }
      this.$api.getMachineMonitorTop({
        machineId: this.machineId,
        name: this.searchProcessName
      }).then(({ data }) => {
        this.processLoading = false
        this.processes = data
      }).catch(() => {
        this.processLoading = false
      })
    },
    reloadSystemLoad() {
      this.$api.getMachineMonitorLoad({
        machineId: this.machineId
      }).then(({ data }) => {
        this.processLoading = false
        this.load = data
      }).catch(() => {
        this.processLoading = false
      })
    },
    loadChart(loading) {
      this.chartLoading = loading
      const params = {
        machineId: this.machineId,
        granularity: 20
      }
      params.endRange = ~~(Date.now() / 1000)
      params.startRange = params.endRange - 3600
      this.loadCpuChart(loading, params)
      this.loadMemoryChart(loading, params)
      this.loadNetChart(loading, params)
    },
    loadCpuChart(loading, params) {
      this.chartLoading = loading
      this.$api.getMachineMonitorCpuChart(params).then(({ data }) => {
        this.chartLoading = false
        const chartData = data.usage.metrics
        this.charts.cpu.max = data.usage.max
        this.charts.cpu.curr = chartData[chartData.length - 1].value
        this.$nextTick(() => {
          timestampRender('cpu-chart', this.charts.cpu, 'chart', text => {
            return formatSecond(text)
          }, text => {
            return parseFloat(text).toFixed(1)
          }, item => {
            item.name = 'CPU使用率'
            item.value = item.value + '%'
          }, chartData)
        })
      }).catch(() => {
        this.chartLoading = false
      })
    },
    loadMemoryChart(loading, params) {
      this.chartLoading = loading
      this.$api.getMachineMonitorMemoryChart(params).then(({ data }) => {
        this.chartLoading = false
        const usageData = data.usage.metrics
        const sizeData = data.size.metrics
        this.charts.memory.currUsage = usageData[usageData.length - 1].value
        this.charts.memory.currSize = sizeData[sizeData.length - 1].value
        this.$nextTick(() => {
          timestampRender('memory-usage-chart', this.charts.memory, 'usageChart', text => {
            return formatSecond(text)
          }, text => {
            return parseFloat(text).toFixed(2)
          }, item => {
            item.name = '内存使用率'
            item.value = item.value + '%'
          }, usageData)
          this.$nextTick(() => {
            timestampRender('memory-size-chart', this.charts.memory, 'sizeChart', text => {
              return formatSecond(text)
            }, text => {
              return parseInt(text)
            }, item => {
              item.name = '内存使用量'
              item.value = item.value + ' MB'
            }, sizeData)
          })
        })
      }).catch(() => {
        this.chartLoading = false
      })
    },
    loadNetChart(loading, params) {
      this.chartLoading = loading
      this.$api.getMachineMonitorNetChart(params).then(({ data }) => {
        this.chartLoading = false
        const sentData = data.sentSpeed.metrics
        const recvData = data.recvSpeed.metrics
        const chartData = []
        for (const sent of sentData) {
          chartData.push({
            ...sent,
            type: '发送'
          })
        }
        for (const recv of recvData) {
          chartData.push({
            ...recv,
            type: '接收'
          })
        }
        this.charts.net.sent = sentData[sentData.length - 1].value
        this.charts.net.recv = recvData[recvData.length - 1].value
        const render = () => {
          let chart = this.charts.net.chart
          const needInit = !chart
          if (needInit) {
            chart = this.charts.net.chart = new Chart({
              container: 'net-chart',
              autoFit: true,
              height: 120
            })
            chart.animate(false)
            chart.axis('time', {
              label: {
                formatter: text => {
                  return formatSecond(text)
                }
              }
            })
            chart.axis('value', {
              label: {
                formatter: text => {
                  return parseFloat(text).toFixed(2)
                }
              }
            })
            chart.tooltip({
              title: (title, datum) => formatSecond(datum.time, 'yyyy-MM-dd HH:mm:ss'),
              customItems: (items) => {
                items[0].value = items[0].value + ' Mbps'
                return items
              }
            })
            chart.legend({
              position: 'top'
            })
            chart.line().position('time*value').color('type').shape('circle')
            chart.data(chartData)
            chart.render()
          } else {
            chart.changeData(chartData)
          }
        }
        this.$nextTick(render)
      }).catch(() => {
        this.chartLoading = false
      })
    },
    preview(value) {
      this.$refs.preview.preview(value)
    }
  },
  created() {
    this.summaryLoading = true
    this.$api.getMachineMonitorMetrics({
      machineId: this.machineId
    }).then(({ data }) => {
      this.summaryLoading = false
      this.summaryData = data
      this.load = data.load
      this.disks = data.disks || []
      this.processes = data.processes || []
      console.log('硬盘数据:', this.disks) // 调试日志，查看数据格式
    }).catch(() => {
      this.summaryLoading = false
      this.summaryData = null
    })
    this.$nextTick(() => {
      this.loadChart(true)
    })
  }
}
</script>

<style lang="less" scoped>
.summary-container {
  padding: 16px;
  overflow: auto;

  .machine-card-wrapper {
    box-shadow: 0 1px 4px 0 #D9D9D9;
    border-radius: 8px;
    transition: all 0.2s;
    
    &:hover {
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    }
  }

  .machine-summary-wrapper {
    margin: 2px 8px 2px 2px;
  }

  .machine-chart-top-wrapper {
    margin-top: 2px;
    margin-bottom: 7px;
  }

  .machine-chart-bottom-wrapper {
    margin-top: 10px;
    margin-bottom: 2px;
  }

  .machine-chart-left-wrapper {
    margin-left: 8px;
    margin-right: 6px;
  }

  .machine-chart-right-wrapper {
    margin-left: 10px;
    margin-right: 2px;
  }

  .machine-summary-info-wrapper {
    width: 100%;
    margin-bottom: 14px;
    display: flex;
    align-items: flex-start;

    .machine-info-label {
      color: #64748B;
      display: inline-block;
      white-space: nowrap;
      font-size: 13px;
      width: 90px;
      flex-shrink: 0;
    }

    .machine-info-value {
      font-size: 13px;
      font-weight: 500;
      color: #0F172A;
      flex: 1;
      word-break: break-word;
    }
  }

  .metrics-floor-1 {
    margin-bottom: 12px;
  }

  .metrics-floor-2 {
    margin-top: 10px;
  }

  .machine-disk-wrapper {
    margin: 2px 8px 2px 2px;
  }

  .machine-processes-wrapper {
    margin: 2px 2px 2px 8px;
  }
}

.card-title {
  color: #0F172A;
  font-weight: 600;
  font-size: 16px;
  margin-bottom: 16px;
  user-select: none;
}

// 硬盘信息样式
.disk-info-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;

  .disk-text-wrapper {
    flex: 1;
    
    .disk-name {
      font-size: 16px;
      font-weight: 600;
      color: #0F172A;
      margin-bottom: 12px;
      word-break: break-word;
    }
    
    .disk-detail {
      .disk-item {
        display: flex;
        justify-content: space-between;
        margin-bottom: 8px;
        
        &:last-child {
          margin-bottom: 0;
        }
        
        .disk-label {
          font-size: 12px;
          color: #64748B;
        }
        
        .disk-value {
          font-size: 13px;
          font-weight: 500;
          color: #1E293B;
        }
      }
    }
  }

  .disk-using-progress {
    flex-shrink: 0;
    
    .disk-using-rate-text {
      font-size: 18px;
      font-weight: 600;
      color: #3B82F6;
    }
  }
}

// 进程区域样式
.processes-title-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;

  .processes-search-wrapper {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .processes-search-input {
    width: 200px;
    border-radius: 8px;
  }

  .processes-search-icon {
    cursor: pointer;
    font-size: 18px;
    color: #64748B;
    transition: color 0.2s;
    
    &:hover {
      color: #3B82F6;
    }
  }
}

// 图表样式
.chart-container {
  width: 100%;
  height: 120px;
}

.chart-header-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 8px;
  flex-wrap: wrap;
  gap: 8px;

  .chart-statistic-wrapper {
    display: flex;
    align-items: baseline;
    gap: 4px;
    flex-wrap: wrap;
  }

  .chart-statistic-label {
    font-size: 11px;
    font-weight: 500;
    color: #64748B;
  }

  .chart-statistic-value {
    font-size: 12px;
    font-weight: 600;
    color: #3B82F6;
    margin-right: 8px;
  }
}

// 表格样式优化
::v-deep .ant-table {
  font-size: 13px;
}

::v-deep .ant-table-thead > tr > th {
  background: #F8FAFF;
  color: #475569;
  font-weight: 600;
}

::v-deep .ant-table-tbody > tr:hover > td {
  background: #F8FAFF;
}
</style>