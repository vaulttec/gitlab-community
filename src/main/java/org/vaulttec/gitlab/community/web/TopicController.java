/*
 * GitLab Community
 * Copyright (c) 2020 Torsten Juergeleit
 * mailto:torsten AT vaulttec DOT org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.vaulttec.gitlab.community.web;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;
import org.vaulttec.gitlab.community.CommunityService;
import org.vaulttec.gitlab.community.model.Member;
import org.vaulttec.gitlab.community.model.Topic;

@Controller
public class TopicController {

  @Autowired
  private CommunityService service;

  @GetMapping("/topics/new")
  public String newTopic(Model model) {
    model.addAttribute("community", service.getCommunity());
    model.addAttribute("create", true);
    model.addAttribute("topic", new Topic());
    return "topic-edit";
  }

  @PostMapping("/topics")
  public String createTopic(Model model, @Valid Topic topic, BindingResult result, HttpServletRequest request) {
    model.addAttribute("community", service.getCommunity());
    if (!request.isUserInRole("ROLE_ADMIN")) {
      model.addAttribute("errorMessage", "You're not authorized");
      return "topic";
    }
    if (result.hasErrors()) {
      model.addAttribute("create", true);
      return "topic-edit";
    }
    if (service.getTopic(topic.getPath()) != null) {
      model.addAttribute("create", true);
      model.addAttribute("errorMessage", "Topic with this path already exists");
      return "topic-edit";
    }
    topic = service.createTopic(topic.getPath(), topic.getName(), topic.getDescription());
    if (topic == null) {
      model.addAttribute("create", true);
      model.addAttribute("errorMessage", "Topic creation failed");
      return "topic-edit";
    }
    return "redirect:/topics/" + topic.getPath();
  }

  @GetMapping("/topics")
  public String getTopics(Model model, Pageable pageable, HttpServletRequest request) {
    model.addAttribute("community", service.getCommunity());
    if (pageable.getSort().isUnsorted()) {
      pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "name"));
    }
    model.addAttribute("member", service.getMember(request.getUserPrincipal().getName()));
    model.addAttribute("topicsPage", service.getTopicsPaged(pageable));
    model.addAttribute("topicMembers", service.getTopicMembers());
    return "topics";
  }

  @GetMapping("/topics/{topicPath}")
  public String getTopic(Model model, @PathVariable("topicPath") String topicPath, HttpServletRequest request) {
    model.addAttribute("community", service.getCommunity());
    if (request.isUserInRole("ROLE_GUEST")) {
      model.addAttribute("errorMessage", "You're not authorized");
      return "topic";
    }
    Topic topic = service.getTopic(topicPath);
    model.addAttribute("topic", topic);
    model.addAttribute("isTopicMember", service.isTopicMember(topic, request.getUserPrincipal().getName()));
    model.addAttribute("members", service.getMembersForTopic(topic));
    model.addAttribute("teamUrl", service.getCommunity().getTeam().getUrl());
    return "topic";
  }

  @GetMapping("/topics/{topicPath}/edit")
  public String editTopic(Model model, @PathVariable("topicPath") String topicPath) {
    model.addAttribute("community", service.getCommunity());
    Topic topic = service.getTopic(topicPath);
    model.addAttribute("create", false);
    model.addAttribute("topic", topic);
    return "topic-edit";
  }

  @PostMapping("/topics/{topicPath}")
  public RedirectView updateTopic(RedirectAttributes attributes, @PathVariable("topicPath") String topicPath,
      Topic topic, HttpServletRequest request) {
    if (!request.isUserInRole("ROLE_ADMIN")) {
      attributes.addFlashAttribute("errorMessage", "You're not authorized");
    } else {
      topic = service.updateTopic(topicPath, topic.getName(), topic.getDescription());
      if (topic == null) {
        attributes.addFlashAttribute("errorMessage", "Topic update failed");
      }
    }
    return new RedirectView("/topics/" + topicPath, true);
  }

  @PostMapping("/topics/{topicPath}/join")
  public RedirectView joinTopic(RedirectAttributes attributes, @PathVariable("topicPath") String topicPath,
      HttpServletRequest request) {
    Topic topic = service.getTopic(topicPath);
    Member member = service.getMember(request.getUserPrincipal().getName());
    if (topic != null && member != null) {
      boolean added = service.addTopicMember(topic, member);
      if (!added) {
        attributes.addFlashAttribute("errorMessage", "Joining topic failed");
      }
    }
    return new RedirectView("/topics/" + topicPath, true);
  }

  @PostMapping("/topics/{topicPath}/leave")
  public RedirectView leaveTopic(RedirectAttributes attributes, @PathVariable("topicPath") String topicPath,
      HttpServletRequest request) {
    Topic topic = service.getTopic(topicPath);
    Member member = service.getMember(request.getUserPrincipal().getName());
    if (topic != null && member != null) {
      boolean left = service.removeTopicMember(topic, member);
      if (!left) {
        attributes.addFlashAttribute("errorMessage", "Leaving topic failed");
      }
    }
    return new RedirectView("/topics/" + topicPath, true);
  }

  @GetMapping("/members/{username}/topics")
  public String getMemberTopics(Model model, @PathVariable("username") String username, Pageable pageable,
      HttpServletRequest request) {
    model.addAttribute("community", service.getCommunity());
    if (pageable.getSort().isUnsorted()) {
      pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "path"));
    }
    if (request.isUserInRole("ROLE_GUEST")) {
      model.addAttribute("errorMessage", "You're not authorized");
      Page<Topic> topicsPage = service.getTopicsPaged(pageable);
      model.addAttribute("topicsPage", topicsPage);
      model.addAttribute("topicMembers", service.getTopicMembers());
      return "topics";
    }
    Member member = service.getMember(username);
    model.addAttribute("member", member);
    if (member == null) {
      Page<Topic> topicsPage = service.getTopicsPaged(pageable);
      model.addAttribute("topicsPage", topicsPage);
      model.addAttribute("errorMessage", "Unknown member - list all topics");
    } else {
      Page<Topic> topicsPage = service.getTopicsForMemberPaged(member, pageable);
      model.addAttribute("topicsPage", topicsPage);
    }
    model.addAttribute("topicMembers", service.getTopicMembers());
    model.addAttribute("isMemberTopics", true);
    return "topics";
  }
}
