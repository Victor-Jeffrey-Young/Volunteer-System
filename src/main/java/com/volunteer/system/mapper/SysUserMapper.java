package com.volunteer.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.volunteer.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> { // 继承 BaseMapper

    @Update("UPDATE sys_user SET password = #{newPassword} " +
            "WHERE user_id = #{userId} AND password = #{oldPassword}")
    int updatePasswordIfCurrent(
            @Param("userId") Long userId,
            @Param("oldPassword") String oldPassword,
            @Param("newPassword") String newPassword);

    /**
     * 原子累加「可用积分 + 总积分 + 总工时」。
     *
     * 替代「getById 读出来 → 内存里相加 → updateById 写回」的写法：
     * 后者在并发下两个事务读到同一个旧值，后写的会覆盖先写的（丢更新），
     * 而且实体里任一字段为 null 时还会抛 NPE。把加法交给数据库一条语句完成，
     * 既天然原子，也不再需要调用方做空值兜底。
     */
    @Update("UPDATE sys_user SET current_points = current_points + #{points}, " +
            "total_points   = total_points   + #{points}, " +
            "total_hours    = total_hours    + #{hours} " +
            "WHERE user_id = #{userId}")
    int addRewards(@Param("userId") Long userId,
                   @Param("points") int points,
                   @Param("hours") BigDecimal hours);

    /**
     * 条件扣减可用积分。
     *
     * WHERE 里带上余额判断，让「检查余额」与「扣减余额」成为一条原子语句：
     * 并发下余额不足的那次会影响到 0 行，由调用方转成业务提示，从而杜绝积分透支。
     */
    @Update("UPDATE sys_user SET current_points = current_points - #{points} " +
            "WHERE user_id = #{userId} AND current_points >= #{points}")
    int deductPointsIfEnough(@Param("userId") Long userId, @Param("points") int points);

    /** 原子自增点赞数（计数场景绝不能读出来加一再整体写回） */
    @Update("UPDATE sys_user SET likes = likes + 1 WHERE user_id = #{userId}")
    int incrementLikes(@Param("userId") Long userId);

    /** 加行级锁读取用户：用于必须在锁内完成「读—判断—写」的场景 */
    @Select("SELECT * FROM sys_user WHERE user_id = #{id} FOR UPDATE")
    SysUser selectByIdForUpdate(Long id);

    /**
     * 刷新「昨日积分排名」快照：一条窗口函数语句算完。
     *
     * 替代原来「把全部志愿者查进内存 → for 循环里逐条 UPDATE」的写法，好处有三：
     *   1. 锁窗口从「遍历全部用户」缩短到「单条 UPDATE」，午夜不再和工时结算抢同一批行锁；
     *   2. 排名由数据库一次算完，不必把整张用户表读进应用内存；
     *   3. 并列时的先后顺序稳定（原来取决于 list() 的返回顺序）。
     *
     * 用 ROW_NUMBER() 而不是 RANK()：保持原实现「1..N 不并列」的语义，
     * 并列时再按 user_id 升序给出确定先后。若产品想要并列同名次，换成 RANK() 即可。
     * 只更新 VOLUNTEER：与原来只查 role='VOLUNTEER' 的行为一致，管理员不参与排名。
     * 该 UPDATE...JOIN 写法已在本机 MySQL 8.0.46 上用临时表验证可用。
     *
     * @return 更新行数
     */
    @Update("UPDATE sys_user u JOIN (" +
            "  SELECT user_id, ROW_NUMBER() OVER (ORDER BY total_points DESC, user_id ASC) AS rk " +
            "  FROM sys_user WHERE role = 'VOLUNTEER'" +
            ") t ON t.user_id = u.user_id " +
            "SET u.last_rank = t.rk")
    int refreshPointsRank();

    /** 刷新「昨日时长排名」快照（写入 last_hours_rank），规则同上，按累计工时降序 */
    @Update("UPDATE sys_user u JOIN (" +
            "  SELECT user_id, ROW_NUMBER() OVER (ORDER BY total_hours DESC, user_id ASC) AS rk " +
            "  FROM sys_user WHERE role = 'VOLUNTEER'" +
            ") t ON t.user_id = u.user_id " +
            "SET u.last_hours_rank = t.rk")
    int refreshHoursRank();
}
