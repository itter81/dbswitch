// Copyright tang.  All rights reserved.
// https://gitee.com/inrgihc/dbswitch
//
// Use of this source code is governed by a BSD-style license
//
// Author: tang (inrgihc@126.com)
// Date : 2020/1/2
// Location: beijing , china
/////////////////////////////////////////////////////////////
package org.dromara.dbswitch.admin.model.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dromara.dbswitch.admin.type.IncludeExcludeEnum;
import org.dromara.dbswitch.admin.type.ScheduleModeEnum;
import org.dromara.dbswitch.common.entity.PatternMapper;
import org.dromara.dbswitch.common.entity.TableColumnPair;
import org.dromara.dbswitch.common.type.CaseConvertEnum;
import org.dromara.dbswitch.common.type.ProductTableEnum;
import org.dromara.dbswitch.common.type.SyncOptionEnum;

@NoArgsConstructor
@Data
@ApiModel("任务详情")
public class AssignmentDetailResponse {

  @ApiModelProperty("ID编号")
  private Long id;

  @ApiModelProperty("任务名")
  private String name;

  @ApiModelProperty("描述")
  private String description;

  @ApiModelProperty("调度模式")
  private ScheduleModeEnum scheduleMode;

  @ApiModelProperty("Cron表达式")
  private String cronExpression;

  @ApiModelProperty("是否已发布")
  private Boolean isPublished;

  @ApiModelProperty("配置信息")
  private Configuration configuration;

  @ApiModelProperty("创建时间")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
  private Timestamp createTime;

  @ApiModelProperty("更新时间")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
  private Timestamp updateTime;

  @NoArgsConstructor
  @Data
  @ApiModel("任务配置")
  public static class Configuration {

    @ApiModelProperty("源端连接ID")
    private Long sourceConnectionId;

    @ApiModelProperty("源端连接名称")
    private String sourceConnectionName;

    @ApiModelProperty("源端数据源类型")
    private String sourceTypeName;

    @ApiModelProperty("源端数据源的Schema")
    private String sourceSchema;

    @ApiModelProperty("源端表类型")
    private ProductTableEnum tableType;

    @ApiModelProperty("表明配置方式")
    private IncludeExcludeEnum includeOrExclude;

    @ApiModelProperty("配置的表名列表")
    private List<String> sourceTables;

    @ApiModelProperty("增量同步表配置")
    private List<TableColumnPair> incrTableColumns;

    @ApiModelProperty("源端同步前置执行SQL脚本")
    private String sourceBeforeSqlScripts;

    @ApiModelProperty("源端同步后置执行SQL脚本")
    private String sourceAfterSqlScripts;

    @ApiModelProperty("目的端连接ID")
    private Long targetConnectionId;

    @ApiModelProperty("目的端连接名称")
    private String targetConnectionName;

    @ApiModelProperty("目的端数据源类型")
    private String targetTypeName;

    @ApiModelProperty("目的端数据源的Schema")
    private String targetSchema;

    @ApiModelProperty("是否只建表")
    private Boolean targetOnlyCreate;

    @ApiModelProperty("是否删除同名表")
    private Boolean targetDropTable;
    @ApiModelProperty("是否每次完全覆盖")
    private Boolean targetAlwaysDrop;
    @ApiModelProperty("是否启用智能容错模式")
    private Boolean smartRecovery;
    @ApiModelProperty("智能容错当前状态(0正常1待恢复)")
    private Integer recoveryState;

    @ApiModelProperty("是否建表允许自增字段")
    private Boolean targetAutoIncrement;

    @ApiModelProperty("表名大小写配置")
    private CaseConvertEnum tableNameCase;

    @ApiModelProperty("列名大小写配置")
    private CaseConvertEnum columnNameCase;

    @ApiModelProperty("表名映射关系")
    private List<PatternMapper> tableNameMapper;

    @ApiModelProperty("字段名映射关系")
    private List<PatternMapper> columnNameMapper;

    @ApiModelProperty("数据批次大小")
    private Integer batchSize;

    @ApiModelProperty("通道队列大小")
    private Integer channelSize;

    @ApiModelProperty("同步操作方法")
    private SyncOptionEnum targetSyncOption;

    @ApiModelProperty("目标端同步前置执行SQL脚本")
    private String targetBeforeSqlScripts;

    @ApiModelProperty("目标端同步后置执行SQL脚本")
    private String targetAfterSqlScripts;

    @ApiModelProperty("自定义建表DDL映射(key:目标表名, value:DDL语句)")
    private Map<String, String> customDdlMap;
  }
}
