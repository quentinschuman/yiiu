package co.yiiu.web.api;

import co.yiiu.core.base.BaseController;
import co.yiiu.core.bean.Result;
import co.yiiu.core.exception.ApiException;
import co.yiiu.module.notification.service.NotificationService;
import co.yiiu.module.reply.model.Reply;
import co.yiiu.module.reply.service.ReplyService;
import co.yiiu.module.topic.model.Topic;
import co.yiiu.module.topic.service.TopicService;
import co.yiiu.module.user.model.User;
import co.yiiu.module.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

/**
 * Created by tomoya.
 * Copyright (c) 2016, All Rights Reserved.
 * https://yiiu.co
 */
@RestController
@RequestMapping("/api/reply")
public class ReplyApiController extends BaseController {

  @Autowired
  private TopicService topicService;
  @Autowired
  private ReplyService replyService;
  @Autowired
  private NotificationService notificationService;
  @Autowired
  private UserService userService;

  @PostMapping("/save")
  public Result save(Integer topicId, String content, String token) throws ApiException {
    User user = getUser(token);
    if (user == null)
      throw new ApiException("用户不存在");
    if (user.isBlock())
      throw new ApiException("你的帐户已经被禁用了，不能进行此项操作");

    if (user.getScore() < 5) throw new ApiException("你的积分不足，不能评论");

    if (topicId == null)
      throw new ApiException("话题ID不能为空");

    if (StringUtils.isEmpty(content))
      throw new ApiException("回复内容不能为空");

    Topic topic = topicService.findById(topicId);
    if (topic == null)
      throw new ApiException("话题不存在");

    Reply reply = new Reply();
    reply.setUser(user);
    reply.setTopic(topic);
    reply.setInTime(new Date());
    reply.setUp(0);
    reply.setContent(content);

    replyService.save(reply);

    // update score
    user.setScore(user.getScore() - 5);
    userService.save(user);

    // 回复+1
    topic.setReplyCount(topic.getReplyCount() + 1);
    topicService.save(topic);

    notificationService.sendNotification(user, topic, content);

    return Result.success();
  }
}