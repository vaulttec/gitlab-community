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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.vaulttec.gitlab.community.CommunityService;
import org.vaulttec.gitlab.community.model.Member;
import org.vaulttec.gitlab.community.model.Topic;

@Controller
public class MemberController {

  @Autowired
  private CommunityService service;

  @GetMapping("/members")
  public String getMembers(Model model, Pageable pageable) {
    model.addAttribute("community", service.getCommunity());
    if (pageable.getSort().isUnsorted()) {
      pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
          Sort.by(Sort.Direction.ASC, "username"));
    }
    model.addAttribute("membersPage", service.getMembersPaged(pageable));
    model.addAttribute("memberTopics", service.getMemberTopics());
    return "members";
  }

  @GetMapping("/members/{username}")
  public String getMember(Model model, @PathVariable("username") String username, HttpServletRequest request) {
    model.addAttribute("community", service.getCommunity());
    if (request.isUserInRole("ROLE_GUEST")) {
      model.addAttribute("errorMessage", "You're not authorized");
      return "member";
    }
    Member member = service.getMember(username);
    model.addAttribute("member", member);
    model.addAttribute("topics", service.getTopicsForMember(member));
    model.addAttribute("teamUrl", service.getCommunity().getTeam().getUrl());
    return "member";
  }

  @GetMapping("/topics/{topicPath}/members")
  public String getTopicMembers(Model model, @PathVariable("topicPath") String topicPath, Pageable pageable,
      HttpServletRequest request) {
    model.addAttribute("community", service.getCommunity());
    if (pageable.getSort().isUnsorted()) {
      pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "path"));
    }
    if (request.isUserInRole("ROLE_GUEST")) {
      model.addAttribute("errorMessage", "You're not authorized");
      Page<Member> membersPage = service.getMembersPaged(pageable);
      model.addAttribute("membersPage", membersPage);
      model.addAttribute("memberTopics", service.getMemberTopics());
      return "members";
    }
    Topic topic = service.getTopic(topicPath);
    model.addAttribute("topic", topic);
    if (topic == null) {
      Page<Member> membersPage = service.getMembersPaged(pageable);
      model.addAttribute("membersPage", membersPage);
      model.addAttribute("errorMessage", "Unknown topic - list all members");
    } else {
      Page<Member> membersPage = service.getMembersForTopicPaged(topic, pageable);
      model.addAttribute("membersPage", membersPage);
    }
    model.addAttribute("memberTopics", service.getMemberTopics());
    model.addAttribute("isTopicMembers", true);
    return "members";
  }
}
