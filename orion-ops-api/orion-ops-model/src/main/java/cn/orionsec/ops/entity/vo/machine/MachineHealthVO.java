package cn.orionsec.ops.entity.vo.machine;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 机器健康度监控 VO
 *
 * @author orion
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "机器健康度监控")
public class MachineHealthVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "机器ID")
    private Long id;

    @ApiModelProperty(value = "机器名称")
    private String name;

    @ApiModelProperty(value = "CPU使用率(%)")
    private Double cpu;

    @ApiModelProperty(value = "内存使用率(%)")
    private Double memory;

    @ApiModelProperty(value = "磁盘使用率(%)")
    private Double disk;

    @ApiModelProperty(value = "健康状态: 正常/注意/告警")
    private String status;

}