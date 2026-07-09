package cn.orionsec.ops.entity.vo.machine;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 机器健康度监控响应
 *
 * @author orion
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "机器健康度监控响应")
public class MachineHealthResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "机器健康度列表")
    private List<MachineHealthVO> rows;

}