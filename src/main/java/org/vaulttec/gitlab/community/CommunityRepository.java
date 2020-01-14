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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.vaulttec.gitlab.community.gitlab.GitLabClient;
import org.vaulttec.gitlab.community.gitlab.GitLabRepository;
import org.vaulttec.gitlab.community.gitlab.model.GLGroup;
import org.vaulttec.gitlab.community.gitlab.model.GLGroupMember;
import org.vaulttec.gitlab.community.gitlab.model.GLUser;
import org.vaulttec.gitlab.community.mattermost.MattermostClient;
import org.vaulttec.gitlab.community.mattermost.model.MMChannel;
import org.vaulttec.gitlab.community.mattermost.model.MMTeam;
import org.vaulttec.gitlab.community.mattermost.model.MMUser;
import org.vaulttec.gitlab.community.model.Community;
import org.vaulttec.gitlab.community.model.Member;
import org.vaulttec.gitlab.community.model.Topic;

@Repository
public class CommunityRepository {
  public static final String GROUP_DESCRIPTION_POSTFIX = " [(About)](";
  public static final String CHANNEL_PURPOSE_PREFIX = "Community topic '";

  private static final Logger LOG = LoggerFactory.getLogger(CommunityRepository.class);

  private final CommunityConfig communityConfig;
  private final GitLabRepository gitLabRepository;
  private final GitLabClient gitLabClient;
  private final MattermostClient mattermostClient;
  private final Community community;

  public CommunityRepository(CommunityConfig communityConfig, GitLabRepository gitLabRepository,
      GitLabClient gitLabClient, MattermostClient mattermostClient) {
    LOG.debug("Initializing CommunityRepository");
    this.communityConfig = communityConfig;
    this.gitLabRepository = gitLabRepository;
    this.gitLabClient = gitLabClient;
    this.mattermostClient = mattermostClient;
    GLGroup group = gitLabClient.getGroup(communityConfig.getGroupPath());
    if (group == null) {
      throw new IllegalStateException("No GitLab group with path '" + communityConfig.getGroupPath() + "' found");
    }
    MMTeam team = mattermostClient.getTeamByName(communityConfig.getGroupPath());
    if (team == null) {
      throw new IllegalStateException("No Mattermost team with name '" + communityConfig.getGroupPath() + "' found");
    }
    this.community = new Community(group, team);
  }

  public Community getCommunity() {
    return community;
  }

  @Cacheable("members")
  public Map<String, Member> getMembers() {
    return retrieveMembers();
  }

  @CachePut("members")
  public Map<String, Member> refreshMembers() {
    LOG.debug("Refreshing members");
    return retrieveMembers();
  }

  private Map<String, Member> retrieveMembers() {
    LOG.debug("Retrieving all members");
    Map<String, Member> members = new HashMap<String, Member>();
    List<GLGroupMember> groupMembers = gitLabClient.getGroupMembers(community.getGroup().getId());
    if (groupMembers != null) {
      groupMembers.forEach(groupMember -> {
        GLUser user = gitLabRepository.getUsers().get(groupMember.getUsername());
        if (user != null) {
          if (!user.hasCustomAttribute(GLUser.CUSTOM_ATTRIBUTE_JOINED)) {
            String today = LocalDate.now().format(GLUser.JOINED_FORMATTER);
            gitLabClient.setUserCustomAttribute(user.getId(), GLUser.CUSTOM_ATTRIBUTE_JOINED, today);
            user.addCustomAttribute(GLUser.CUSTOM_ATTRIBUTE_JOINED, today);
          }
          members.put(user.getUsername(),
              new Member(user, communityConfig.getAdminUsernames().contains(user.getUsername())));
        }
      });
    }
    return members;
  }

  @Cacheable("topics")
  public Map<String, Topic> getTopics() {
    return retrieveTopics();
  }

  @CachePut("topics")
  public Map<String, Topic> refreshTopics() {
    LOG.debug("Refreshing topics");
    return retrieveTopics();
  }

  private Map<String, Topic> retrieveTopics() {
    LOG.debug("Retrieving all topics");
    Map<String, Topic> topics = new HashMap<String, Topic>();
    List<GLGroup> groups = gitLabClient.getSubGroups(community.getGroup().getId());
    if (groups != null) {
      groups.forEach(group -> {
        MMChannel channel = mattermostClient.getChannelByName(community.getTeam(), group.getPath());
        Topic topic = new Topic(group, channel);
        removeDescriptionPostfix(topic);
        topics.put(topic.getPath(), topic);
      });
    }
    return topics;
  }

  private void removeDescriptionPostfix(Topic topic) {
    int descriptionPostfixIndex = topic.getDescription().indexOf(GROUP_DESCRIPTION_POSTFIX);
    if (descriptionPostfixIndex >= 0) {
      topic.setDescription(topic.getDescription().substring(0, descriptionPostfixIndex));
    }
  }

  public Topic createTopic(String path, String name, String description) {
    LOG.debug("Creating new topic: path={}, name={}", path, name);

    // Build description with topic link
    ServletUriComponentsBuilder uriBuilder = ServletUriComponentsBuilder.fromCurrentContextPath();
    uriBuilder.removePathExtension();
    String topicUri = uriBuilder.path("/topics/" + path).build().toUriString();
    String descriptionWithTopicUri = description + GROUP_DESCRIPTION_POSTFIX + topicUri + ")";

    // Create GitLab sub-group with description (including topic link)
    GLGroup group = gitLabClient.createSubGroup(community.getGroup().getId(), path, name, descriptionWithTopicUri);
    if (group != null) {

      // First check if channel was created previously
      MMChannel channel = mattermostClient.getChannelByName(community.getTeam(), path);
      String purpose = CHANNEL_PURPOSE_PREFIX + path + "'";
      if (channel != null) {

        // Restore and update existing Mattermost channel
        if (channel.getDeleteAt() != null) {
          mattermostClient.restoreChannel(channel);
        }
        mattermostClient.updateChannel(channel, path, name, purpose, descriptionWithTopicUri);
        if (!channel.isPrivate()) {
          mattermostClient.convertChannelIntoPrivate(channel);
        }
      } else {
        channel = mattermostClient.createChannel(community.getTeam(), path, name, purpose, descriptionWithTopicUri,
            true);
      }
      if (channel != null) {

        // Store corresponding Mattermost channel ID in newly created GitLab sub-group
        gitLabClient.setGroupCustomAttribute(group.getId(), GLGroup.CUSTOM_ATTRIBUTE_MATTERMOST_CHANNEL,
            channel.getId());
        group.addCustomAttribute(GLGroup.CUSTOM_ATTRIBUTE_MATTERMOST_CHANNEL, channel.getId());
      }
      return new Topic(group, channel);
    }
    return null;
  }

  public Topic updateTopic(Topic topic, String path, String name, String description) {
    LOG.debug("Updating topic: path={}, name={}, description={}", path, name, description);

    // Build description with topic link
    ServletUriComponentsBuilder uriBuilder = ServletUriComponentsBuilder.fromCurrentContextPath();
    uriBuilder.removePathExtension();
    String topicUri = uriBuilder.path("/topics/" + path).build().toUriString();
    String descriptionWithTopicUri = description + GROUP_DESCRIPTION_POSTFIX + topicUri + ")";

    // Update GitLab sub-group with description (including topic link)
    GLGroup group = gitLabClient.updateGroup(topic.getGroupId(), path, name, descriptionWithTopicUri);
    if (group != null) {

      // Update Mattermost channel
      MMChannel channel = mattermostClient.getChannelByName(community.getTeam(), path);
      String purpose = CHANNEL_PURPOSE_PREFIX + path + "'";
      if (channel != null) {
        mattermostClient.updateChannel(channel, path, name, purpose, descriptionWithTopicUri);
      }
      topic = new Topic(group, channel);
      removeDescriptionPostfix(topic);
      return topic;
    }
    return null;
  }

  public boolean deleteTopic(Topic topic) {
    LOG.debug("Deleting topic {}", topic);
    if (gitLabClient.deleteGroup(topic.getGroupId())) {
      MMChannel channel = mattermostClient.getChannelByName(community.getTeam(), topic.getPath());
      if (channel != null) {
        mattermostClient.deleteChannel(channel);
      }
      return true;
    }
    return false;
  }

  @Cacheable("topicMembers")
  public Map<String, Set<Member>> getTopicMembers() {
    return retrieveTopicMembers();
  }

  @CachePut("topicMembers")
  public Map<String, Set<Member>> refreshTopicMembers() {
    LOG.debug("Refreshing topic members");
    return retrieveTopicMembers();
  }

  private Map<String, Set<Member>> retrieveTopicMembers() {
    LOG.debug("Retrieving members for all topics");
    Map<String, Set<Member>> topicMembers = new HashMap<String, Set<Member>>();
    List<GLGroup> groups = gitLabClient.getSubGroups(community.getGroup().getId());
    if (groups != null) {
      groups.forEach(group -> {
        Set<Member> members = new HashSet<Member>();
        List<GLGroupMember> groupMembers = gitLabClient.getGroupMembers(group.getId());
        if (groupMembers != null) {
          groupMembers.forEach(groupMember -> {
            GLUser user = gitLabRepository.getUsers().get(groupMember.getUsername());
            if (user != null && communityConfig.getTopicPermission().equals(groupMember.getPermission())) {
              members.add(new Member(user, communityConfig.getAdminUsernames().contains(user.getUsername())));
            }
          });
        }
        topicMembers.put(group.getPath(), members);
      });
    }
    return topicMembers;
  }

  public boolean addTopicMember(Topic topic, Member member) {
    LOG.debug("Adding member '{}' to topic '{}'", member.getUsername(), topic.getPath());
    if (gitLabClient.addMemberToGroup(topic.getGroupId(), member.getUserId(), communityConfig.getTopicPermission())) {
      MMChannel channel = mattermostClient.getChannelByName(community.getTeam(), topic.getPath());
      MMUser user = mattermostClient.getUserByUsername(member.getUsername());
      if (channel != null && user != null) {
        mattermostClient.addMemberToChannel(channel, user);
      }
      return true;
    }
    return false;
  }

  public boolean removeTopicMember(Topic topic, Member member) {
    LOG.debug("Removing member '{}' from topic '{}'", member.getUsername(), topic.getPath());
    if (gitLabClient.removeMemberFromGroup(topic.getGroupId(), member.getUserId())) {
      MMChannel channel = mattermostClient.getChannelByName(community.getTeam(), topic.getPath());
      MMUser user = mattermostClient.getUserByUsername(member.getUsername());
      if (channel != null && user != null) {
        mattermostClient.removeMemberFromChannel(channel, user.getId());
      }
      return true;
    }
    return false;
  }
}
