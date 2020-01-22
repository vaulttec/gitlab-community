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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.vaulttec.gitlab.community.CommunityService;
import org.vaulttec.gitlab.community.model.Community;

@Controller
public class CommunityController {

  @Autowired
  private CommunityService service;

  @GetMapping("/")
  public String community(Model model, HttpServletRequest request) {
    Community community = service.getCommunity();
    model.addAttribute("community", community);
    model.addAttribute("isCommunityMember", service.isMember(request.getUserPrincipal().getName()));
    model.addAttribute("teamUrl", service.getCommunity().getTeam().getUrl());
    model.addAttribute("topics", service.getTopics());
    model.addAttribute("members", service.getMembers());
    model.addAttribute("teamUrl", service.getCommunity().getTeam().getUrl());
    return "community";
  }
}
