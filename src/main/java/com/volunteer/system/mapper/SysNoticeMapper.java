package com.volunteer.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.volunteer.system.entity.SysNotice;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysNoticeMapper extends BaseMapper<SysNotice> {
    /**
     * 🚨 修复 BindingException：实装企业级关联查询 SQL
     * 逻辑：左连接已读表，并根据标题关键词进行模糊过滤
     */
    @Select("<script>" +
            "SELECT n.*, (CASE WHEN r.id IS NOT NULL THEN 1 ELSE 0 END) as is_read " +
            "FROM sys_notice n " +
            "LEFT JOIN sys_notice_read r ON n.notice_id = r.notice_id AND r.user_id = #{userId} " +
            "WHERE 1=1 " +
            "<if test='title != null and title != \"\"'>" +
            "  AND n.title LIKE CONCAT('%', #{title}, '%') " +
            "</if>" +
            "ORDER BY n.create_time DESC" +
            "</script>")
    IPage<SysNotice> selectNoticePageWithStatus(IPage<SysNotice> page,
                                                @Param("userId") Long userId,
                                                @Param("title") String title);

    /**
     * 标记单条已读的辅助方法
     */
    @Insert("INSERT IGNORE INTO sys_notice_read (user_id, notice_id, read_time) " +
            "VALUES (#{userId}, #{noticeId}, NOW())")
    int insertReadRecord(@Param("userId") Long userId, @Param("noticeId") Long noticeId);

}