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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;
import org.vaulttec.gitlab.community.model.Community;
import org.vaulttec.gitlab.community.model.Member;
import org.vaulttec.gitlab.community.model.Topic;

@Service
public class CommunityService {

  private final CommunityRepository communityRepository;
  private final Community community;

  public CommunityService(CommunityRepository communityRepository) {
    this.communityRepository = communityRepository;
    this.community = communityRepository.getCommunity();
  }

  public Community getCommunity() {
    return community;
  }

  public String getGroupPath() {
    return community.getGroup().getPath();
  }

  public int getMemberCount() {
    return communityRepository.getMembers().size();
  }

  public Collection<Member> getMembers() {
    return communityRepository.getMembers().values();
  }

  public Page<Member> getMembersPaged(Pageable pageable) {
    return getPagedMembers(getMembers(), pageable);
  }

  private Page<Member> getPagedMembers(Collection<Member> members, Pageable pageable) {
    int pageSize = pageable.getPageSize();
    int currentPage = pageable.getPageNumber();
    int startItem = currentPage * pageSize;
    final Sort sort = pageable.getSort();
    List<Member> sortedMembers;
    if (members.size() < startItem) {
      sortedMembers = Collections.emptyList();
    } else {
      sortedMembers = members.stream().sorted((member1, member2) -> {
        int result = 0;
        if (sort.isSorted()) {
          Order order = sort.get().findFirst().get();
          switch (order.getProperty()) {
          case "username":
            result = member1.getUsername().compareTo(member2.getUsername());
            break;
          case "name":
            result = member1.getName().compareTo(member2.getName());
            break;
          }
          if (order.isDescending()) {
            result = -result;
          }
        }
        return result;
      }).skip(startItem).limit(pageSize).collect(Collectors.toList());
    }
    return new PageImpl<Member>(sortedMembers, PageRequest.of(currentPage, pageSize), members.size());
  }

  public Member getMember(String username) {
    Map<String, Member> members = communityRepository.getMembers();
    return members.get(username);
  }

  public int getTopicCount() {
    return communityRepository.getTopics().size();
  }

  public Collection<Topic> getTopics() {
    return communityRepository.getTopics().values();
  }

  public Page<Topic> getTopicsPaged(Pageable pageable) {
    return getPagedTopics(getTopics(), pageable);
  }

  private Page<Topic> getPagedTopics(Collection<Topic> topics, Pageable pageable) {
    int pageSize = pageable.getPageSize();
    int currentPage = pageable.getPageNumber();
    int startItem = currentPage * pageSize;
    final Sort sort = pageable.getSort();
    List<Topic> sortedTopics;
    if (topics.size() < startItem) {
      sortedTopics = Collections.emptyList();
    } else {
      sortedTopics = topics.stream().sorted((topic1, topic2) -> {
        int result = 0;
        if (sort.isSorted()) {
          Order order = sort.get().findFirst().get();
          switch (order.getProperty()) {
          case "path":
            result = topic1.getPath().compareTo(topic2.getPath());
            break;
          case "name":
            result = topic1.getName().compareTo(topic2.getName());
            break;
          case "description":
            result = topic1.getDescription().compareTo(topic2.getDescription());
            break;
          case "messageCount":
            result = Integer.compareUnsigned(topic1.getMessageCount(), topic2.getMessageCount());
            break;
          case "lastPostAt":
            result = topic1.getLastPostAt().compareTo(topic2.getLastPostAt());
            break;
          }
          if (order.isDescending()) {
            result = -result;
          }
        }
        return result;
      }).skip(startItem).limit(pageSize).collect(Collectors.toList());
    }
    return new PageImpl<Topic>(sortedTopics, PageRequest.of(currentPage, pageSize), topics.size());
  }

  public Topic getTopic(String path) {
    Map<String, Topic> topics = communityRepository.getTopics();
    return topics.get(path);
  }

  public Topic createTopic(String path, String name, String description) {
    Topic topic = communityRepository.createTopic(path, name, description);
    if (topic != null) {
      communityRepository.getTopics().put(topic.getPath(), topic);
      communityRepository.getTopicMembers().put(topic.getPath(), new HashSet<Member>());
    }
    return topic;
  }

  public Topic updateTopic(String path, String name, String description) {
    Map<String, Topic> topics = communityRepository.getTopics();
    Topic topic = topics.get(path);
    if (topic != null) {
      topic = communityRepository.updateTopic(topic, path, name, description);
      if (topic != null) {
        topics.put(topic.getPath(), topic);
      }
    }
    return topic;
  }

  public boolean deleteTopic(Topic topic) {
    boolean deleted = communityRepository.deleteTopic(topic);
    if (deleted) {
      communityRepository.getTopics().remove(topic.getPath());
      communityRepository.getTopicMembers().remove(topic.getPath());
    }
    return deleted;
  }

  public Map<String, Set<Member>> getTopicMembers() {
    return communityRepository.getTopicMembers();
  }

  public Collection<Member> getMembersForTopic(Topic topic) {
    return communityRepository.getTopicMembers().get(topic.getPath());
  }

  public Page<Member> getMembersForTopicPaged(Topic topic, Pageable pageable) {
    return getPagedMembers(getMembersForTopic(topic), pageable);
  }

  public boolean addTopicMember(Topic topic, Member member) {
    boolean added = false;
    Map<String, Set<Member>> topicMembers = communityRepository.getTopicMembers();
    if (topicMembers.containsKey(topic.getPath())) {
      added = communityRepository.addTopicMember(topic, member);
      if (added) {
        HashSet<Member> members = new HashSet<Member>(topicMembers.get(topic.getPath()));
        members.add(member);
        topicMembers.put(topic.getPath(), members);
      }
    }
    return added;
  }

  public boolean removeTopicMember(Topic topic, Member member) {
    boolean removed = false;
    Map<String, Set<Member>> topicMembers = communityRepository.getTopicMembers();
    if (topicMembers.containsKey(topic.getPath())) {
      removed = communityRepository.removeTopicMember(topic, member);
      if (removed) {
        HashSet<Member> members = new HashSet<Member>(topicMembers.get(topic.getPath()));
        members.remove(member);
        topicMembers.put(topic.getPath(), members);
      }
    }
    return removed;
  }

  public boolean isTopicMember(Topic topic, String username) {
    Set<Member> members = communityRepository.getTopicMembers().get(topic.getPath());
    if (members != null) {
      return members.stream().filter(member -> member.getUsername().equals(username)).findFirst().isPresent();
    }
    return false;
  }

  public Collection<Topic> getMemberTopics(final Member member) {
    final Map<String, Topic> topics = communityRepository.getTopics();
    return communityRepository.getTopicMembers().entrySet().stream().filter(entry -> entry.getValue().contains(member))
        .map(entry -> topics.get(entry.getKey())).collect(Collectors.toList());
  }

  public Page<Topic> getMemberTopicsPaged(Member member, Pageable pageable) {
    return getPagedTopics(getMemberTopics(member), pageable);
  }
}
