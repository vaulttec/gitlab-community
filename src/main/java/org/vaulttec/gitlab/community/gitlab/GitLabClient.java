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
package org.vaulttec.gitlab.community.gitlab;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.vaulttec.gitlab.community.gitlab.model.GLGroup;
import org.vaulttec.gitlab.community.gitlab.model.GLGroupMember;
import org.vaulttec.gitlab.community.gitlab.model.GLPermission;
import org.vaulttec.gitlab.community.gitlab.model.GLUser;
import org.vaulttec.http.client.AbstractRestClient;
import org.vaulttec.http.client.LinkHeader;

@Service
public class GitLabClient extends AbstractRestClient {

  private static final Logger LOG = LoggerFactory.getLogger(GitLabClient.class);

  protected static final ParameterizedTypeReference<GLUser> RESPONSE_TYPE_USER = new ParameterizedTypeReference<GLUser>() {
  };
  protected static final ParameterizedTypeReference<List<GLUser>> RESPONSE_TYPE_USERS = new ParameterizedTypeReference<List<GLUser>>() {
  };
  protected static final ParameterizedTypeReference<GLGroup> RESPONSE_TYPE_GROUP = new ParameterizedTypeReference<GLGroup>() {
  };
  protected static final ParameterizedTypeReference<List<GLGroup>> RESPONSE_TYPE_GROUPS = new ParameterizedTypeReference<List<GLGroup>>() {
  };
  protected static final ParameterizedTypeReference<List<GLGroupMember>> RESPONSE_TYPE_GROUP_MEMBERS = new ParameterizedTypeReference<List<GLGroupMember>>() {
  };

  GitLabClient(GitLabClientConfig config, RestTemplateBuilder restTemplateBuilder) {
    super(config, restTemplateBuilder);
    prepareAuthenticationEntity("PRIVATE-TOKEN", config.getPersonalAccessToken());
  }

  public List<GLUser> getActiveUsers() {
    LOG.debug("Retrieving all active users");
    String apiCall = "/users?active=true&with_custom_attributes=true";
    Map<String, String> uriVariables = createUriVariables();
    return makeReadListApiCall(apiCall, HttpMethod.GET, RESPONSE_TYPE_USERS, uriVariables);
  }

  public boolean setUserCustomAttribute(String userId, String key, String value) {
    if (!StringUtils.hasText(userId)) {
      throw new IllegalStateException("GitLab user ID required");
    }
    if (!StringUtils.hasText(key) || !StringUtils.hasText(value)) {
      throw new IllegalStateException("Attribute key/value pair required");
    }
    LOG.debug("Setting custom attribute '{}' ({}) to user {}", key, value, userId);
    String apiCall = "/users/{userId}/custom_attributes/{key}";
    Map<String, String> uriVariables = createUriVariables("userId", userId, "key", key);
    HttpEntity<String> entity = new HttpEntity<String>("{\"value\" : \"" + value + "\"}",
        authenticationEntity.getHeaders());
    return makeWriteApiCall(apiCall, HttpMethod.PUT, entity, uriVariables);
  }

  public GLGroup getGroup(String groupPath) {
    LOG.debug("Retrieving group {}", groupPath);
    String apiCall = "/groups/{groupPath}?with_custom_attributes=true";
    Map<String, String> uriVariables = createUriVariables("groupPath", groupPath);
    return makeReadApiCall(apiCall, HttpMethod.GET, RESPONSE_TYPE_GROUP, uriVariables);
  }

  public List<GLGroup> getSubGroups(String groupId) {
    LOG.debug("Retrieving subgroups for group {}", groupId);
    String apiCall = "/groups/{groupId}/subgroups?with_custom_attributes=true";
    Map<String, String> uriVariables = createUriVariables("groupId", groupId);
    return makeReadListApiCall(apiCall, HttpMethod.GET, RESPONSE_TYPE_GROUPS, uriVariables);
  }

  public GLGroup createSubGroup(String parentGroupId, String groupPath, String groupName, String groupDescription) {
    LOG.debug("Creating subgroup '{}' ({}) for parent group {}", groupPath, groupName, parentGroupId);
    String apiCall = "/groups?path={groupPath}&name={groupName}&description={groupDescription}&parent_id={parentGroupId}";
    Map<String, String> uriVariables = createUriVariables("groupPath", groupPath, "groupName", groupName,
        "groupDescription", groupDescription, "parentGroupId", parentGroupId);
    return makeWriteApiCall(apiCall, HttpMethod.POST, RESPONSE_TYPE_GROUP, uriVariables);
  }

  public GLGroup updateGroup(String groupId, String groupPath, String groupName, String groupDescription) {
    LOG.debug("Updating group {} '{}' ({})", groupId, groupPath, groupName);
    String apiCall = "/groups/{groupId}?path={groupPath}&name={groupName}&description={groupDescription}";
    Map<String, String> uriVariables = createUriVariables("groupId", groupId, "groupPath", groupPath, "groupName",
        groupName, "groupDescription", groupDescription);
    return makeWriteApiCall(apiCall, HttpMethod.PUT, RESPONSE_TYPE_GROUP, uriVariables);
  }

  public List<GLGroupMember> getGroupMembers(String groupId) {
    if (!StringUtils.hasText(groupId)) {
      throw new IllegalStateException("GitLab group ID required");
    }
    LOG.debug("Retrieving members for group {}", groupId);
    String apiCall = "/groups/{groupId}/members";
    Map<String, String> uriVariables = createUriVariables("groupId", groupId);
    return makeReadListApiCall(apiCall, HttpMethod.GET, RESPONSE_TYPE_GROUP_MEMBERS, uriVariables);
  }

  public boolean addMemberToGroup(String groupId, String userId, GLPermission permission) {
    if (!StringUtils.hasText(groupId)) {
      throw new IllegalStateException("GitLab group ID required");
    }
    if (!StringUtils.hasText(userId)) {
      throw new IllegalStateException("GitLab user ID required");
    }
    LOG.info("Adding user '{}' to group '{}' as {}", userId, groupId, permission);
    String apiCall = "/groups/{groupId}/members?user_id={userId}&access_level={accessLevel}";
    Map<String, String> uriVariables = createUriVariables("groupId", groupId, "userId", userId, "accessLevel",
        permission.getAccessLevel());
    return makeWriteApiCall(apiCall, HttpMethod.POST, uriVariables);
  }

  public boolean removeMemberFromGroup(String groupId, String userId) {
    if (!StringUtils.hasText(groupId)) {
      throw new IllegalStateException("GitLab group ID required");
    }
    if (!StringUtils.hasText(userId)) {
      throw new IllegalStateException("GitLab user ID required");
    }
    LOG.info("Removing user '{}' from group '{}'", userId, groupId);
    String apiCall = "/groups/{groupId}/members/{userId}";
    Map<String, String> uriVariables = createUriVariables("groupId", groupId, "userId", userId);
    return makeWriteApiCall(apiCall, HttpMethod.DELETE, uriVariables);
  }

  public boolean deleteGroup(String groupId) {
    if (!StringUtils.hasText(groupId)) {
      throw new IllegalStateException("GitLab group ID required");
    }
    LOG.debug("Deleting group {}", groupId);
    String apiCall = "/groups/{groupId}";
    Map<String, String> uriVariables = createUriVariables("groupId", groupId);
    return makeWriteApiCall(apiCall, HttpMethod.DELETE, uriVariables);
  }

  public boolean setGroupCustomAttribute(String groupId, String key, String value) {
    if (!StringUtils.hasText(groupId)) {
      throw new IllegalStateException("GitLab group ID required");
    }
    if (!StringUtils.hasText(key) || !StringUtils.hasText(value)) {
      throw new IllegalStateException("Attribute key/value pair required");
    }
    LOG.debug("Setting custom attribute '{}' ({}) to group {}", key, value, groupId);
    String apiCall = "/groups/{groupId}/custom_attributes/{key}";
    Map<String, String> uriVariables = createUriVariables("groupId", groupId, "key", key);
    HttpEntity<String> entity = new HttpEntity<String>("{\"value\" : \"" + value + "\"}",
        authenticationEntity.getHeaders());
    return makeWriteApiCall(apiCall, HttpMethod.PUT, entity, uriVariables);
  }

  @Override
  protected <T> List<T> makeReadListApiCall(String apiCall, HttpMethod method,
      ParameterizedTypeReference<List<T>> typeReference, Map<String, String> uriVariables, HttpStatus... ignoreStatus) {
    String url = getApiUrl(apiCall + (apiCall.contains("?") ? "&" : "?") + "per_page={perPage}");
    uriVariables.put("perPage", perPageAsString());
    try {
      List<T> entities;
      ResponseEntity<List<T>> response = restTemplate.exchange(url, method, authenticationEntity, typeReference,
          uriVariables);
      LinkHeader linkHeader = LinkHeader.parse(response.getHeaders());
      if (linkHeader == null || !linkHeader.hasLink(LinkHeader.Rel.NEXT)) {
        entities = response.getBody();
      } else {
        entities = new ArrayList<>(response.getBody());
        do {
          URI nextResourceUri = linkHeader.getLink(LinkHeader.Rel.NEXT).getResourceUri();
          response = restTemplate.exchange(nextResourceUri, method, authenticationEntity, typeReference);
          entities.addAll(response.getBody());
          linkHeader = LinkHeader.parse(response.getHeaders());
        } while (linkHeader != null && linkHeader.hasLink(LinkHeader.Rel.NEXT));
      }
      return entities;
    } catch (RestClientException e) {
      logException(method, uriVariables, url, e, ignoreStatus);
    }
    return null;
  }
}
