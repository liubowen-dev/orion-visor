<!-- src/components/command/MetricCard.vue -->
<template>
  <div class="metric-card" :class="`metric-${type}`">
    <div class="metric-icon">
      <a-icon :type="icon" />
    </div>
    <div class="metric-info">
      <div class="metric-value">
        <span v-if="prefix">{{ prefix }}</span>
        <count-up 
          v-if="animate"
          :startVal="0" 
          :endVal="value" 
          :duration="1.2"
          :decimals="decimals"
        />
        <span v-else>{{ formatValue(value) }}</span>
        <span v-if="suffix">{{ suffix }}</span>
      </div>
      <div class="metric-label">{{ label }}</div>
      <div class="metric-trend" v-if="trend !== undefined">
        <a-icon :type="trend >= 0 ? 'arrow-up' : 'arrow-down'" />
        <span :class="{ up: trend >= 0, down: trend < 0 }">
          {{ Math.abs(trend) }}%
        </span>
        <span class="trend-label">较昨日</span>
      </div>
    </div>
  </div>
</template>

<script>
import CountUp from 'vue-countup-v2'

export default {
  name: 'MetricCard',
  components: { CountUp },
  props: {
    label: String,
    value: Number,
    icon: { type: String, default: 'dashboard' },
    prefix: { type: String, default: '' },
    suffix: { type: String, default: '' },
    decimals: { type: Number, default: 0 },
    trend: Number,
    type: { type: String, default: 'default' },
    animate: { type: Boolean, default: true }
  },
  methods: {
    formatValue(value) {
      if (this.decimals === 0) {
        return value.toLocaleString()
      }
      return value.toFixed(this.decimals)
    }
  }
}
</script>

<style lang="less" scoped>
.metric-card {
  background: white;
  border-radius: 20px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.3s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05), 0 0 0 1px rgba(0, 0, 0, 0.02);
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 25px rgba(59, 130, 246, 0.08), 0 0 0 1px rgba(59, 130, 246, 0.1);
  }
  
  .metric-icon {
    width: 52px;
    height: 52px;
    background: #F0F9FF;
    border-radius: 18px;
    display: flex;
    align-items: center;
    justify-content: center;
    
    i {
      font-size: 28px;
      color: #3B82F6;
    }
  }
  
  .metric-info {
    flex: 1;
    
    .metric-value {
      font-size: 32px;
      font-weight: 700;
      color: #0F172A;
      line-height: 1.2;
      font-family: 'Monaco', 'Courier New', monospace;
    }
    
    .metric-label {
      font-size: 13px;
      color: #64748B;
      margin-top: 6px;
    }
    
    .metric-trend {
      font-size: 12px;
      margin-top: 6px;
      display: flex;
      align-items: center;
      gap: 4px;
      
      .up {
        color: #10B981;
      }
      
      .down {
        color: #EF4444;
      }
      
      .trend-label {
        color: #94A3B8;
      }
    }
  }
  
  &.metric-critical .metric-icon {
    background: #FEF2F2;
    i { color: #EF4444; }
  }
  
  &.metric-warning .metric-icon {
    background: #FFFBEB;
    i { color: #F59E0B; }
  }
  
  &.metric-success .metric-icon {
    background: #F0FDF4;
    i { color: #10B981; }
  }
}
</style>