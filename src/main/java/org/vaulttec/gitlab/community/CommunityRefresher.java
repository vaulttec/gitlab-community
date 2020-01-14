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
package org.vaulttec.gitlab.community;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.vaulttec.gitlab.community.gitlab.GitLabRepository;
import org.vaulttec.gitlab.community.mattermost.MattermostClient;
import org.vaulttec.gitlab.community.mattermost.MattermostRepository;
import org.vaulttec.gitlab.community.mattermost.model.MMChannel;
import org.vaulttec.gitlab.community.mattermost.model.MMMember;
import org.vaulttec.gitlab.community.mattermost.model.MMTeam;
import org.vaulttec.gitlab.community.mattermost.model.MMUser;

@Configuration
public class CommunityRefresher {
  private static final Logger LOG = LoggerFactory.getLogger(CommunityRefresher.class);

  @Autowired
  private CommunityRepository communityRepository;
  @Autowired
  private GitLabRepository gitLabRepository;
  @Autowired
  private MattermostRepository mattermostRepository;
  @Autowired
  private MattermostClient mattermostClient;

  @Scheduled(fixedRateString = "${community.refresh-rate}")
  public void refresh() {
    refreshCaches();
    refreshMattermostTeamMembers();
    refreshMattermostChannels();
  }

  private void refreshCaches() {
    LOG.debug("Refreshing caches");
    gitLabRepository.refreshUsers();
    mattermostRepository.refreshUsers();
    communityRepository.refreshMembers();
    communityRepository.refreshTopics();
    communityRepository.refreshTopicMembers();
  }

  private void refreshMattermostTeamMembers() {
    LOG.debug("Refreshing team members");

    // Get a map with current users in Mattermost team
    MMTeam team = communityRepository.getCommunity().getTeam();
    List<MMMember> members = mattermostClient.getTeamMembers(team);
    if (members != null) {
      HashMap<String, MMUser> teamUsers = new HashMap<String, MMUser>();
      members.forEach(member -> {
        MMUser user = mattermostRepository.getUsers().get(member.getUserId());
        if (user != null) {
          teamUsers.put(user.getUsername(), user);
        }
      });

      // Add missing team members by comparing current users in Mattermost team with
      // list of community members
      communityRepository.getMembers().values().forEach(member -> {
        if (teamUsers.remove(member.getUsername()) == null) {
          MMUser missingUser = mattermostRepository.getUsers().values().stream()
              .filter(user -> user.getUsername().equals(member.getUsername())).findAny().orElse(null);
          if (missingUser != null) {
            mattermostClient.addMemberToTeam(team, missingUser);
          }
        }
      });

      // Remove all users in Mattermost team which are not community members
      teamUsers.values().forEach(user -> mattermostClient.removeMemberFromTeam(team, user));
    }
  }

  private void refreshMattermostChannels() {
    LOG.debug("Refreshing topic channels");
    communityRepository.getTopics().values().forEach(topic -> {
      MMChannel channel = mattermostClient.getChannelByName(communityRepository.getCommunity().getTeam(),
          topic.getPath());
      if (channel != null) {
        String purpose = CommunityRepository.CHANNEL_PURPOSE_PREFIX + topic.getPath() + "'";

        // Restore deleted channel
        if (channel.getDeleteAt() != null) {
          mattermostClient.restoreChannel(channel);
        }

        // Restore channel name, description and header
        if (!topic.getPath().equals(channel.getName()) || !topic.getName().equals(channel.getDisplayName())
            || !purpose.equals(channel.getPurpose()) || !topic.getDescription().equals(channel.getHeader())) {
          mattermostClient.updateChannel(channel, topic.getPath(), topic.getName(), purpose, topic.getDescription());
        }
        // Restore left channel members
        List<String> usernames = communityRepository.getTopicMembers().get(topic.getPath()).stream()
            .map(member -> member.getUsername()).collect(Collectors.toList());
        List<MMUser> requiredUsers = mattermostClient.getUsersByUsernames(usernames);
        List<MMMember> currentMembers = mattermostClient.getChannelMembers(channel);
        requiredUsers.forEach(requiredUser -> {
          Optional<MMMember> currentMember = currentMembers.stream()
              .filter(member -> requiredUser.getId().equals(member.getUserId())).findFirst();
          if (currentMember.isPresent()) {
            currentMembers.remove(currentMember.get());
          } else {
            mattermostClient.addMemberToChannel(channel, requiredUser);
          }
        });

        // Remove illegal channel members
        currentMembers.forEach(member -> {
          mattermostClient.removeMemberFromChannel(channel, member.getUserId());
        });
      }
    });
  }
}
