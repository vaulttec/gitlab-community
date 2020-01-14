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
package org.vaulttec.gitlab.community.mattermost;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.vaulttec.gitlab.community.mattermost.model.MMChannel;
import org.vaulttec.gitlab.community.mattermost.model.MMMember;
import org.vaulttec.gitlab.community.mattermost.model.MMTeam;
import org.vaulttec.gitlab.community.mattermost.model.MMUser;
import org.vaulttec.http.client.AbstractRestClient;

@Service
public class MattermostClient extends AbstractRestClient {

  private static final Logger LOG = LoggerFactory.getLogger(MattermostClient.class);

  protected static final ParameterizedTypeReference<MMTeam> RESPONSE_TYPE_TEAM = new ParameterizedTypeReference<MMTeam>() {
  };
  protected static final ParameterizedTypeReference<MMUser> RESPONSE_TYPE_USER = new ParameterizedTypeReference<MMUser>() {
  };
  protected static final ParameterizedTypeReference<List<MMUser>> RESPONSE_TYPE_USERS = new ParameterizedTypeReference<List<MMUser>>() {
  };
  protected static final ParameterizedTypeReference<List<MMMember>> RESPONSE_TYPE_MEMBERS = new ParameterizedTypeReference<List<MMMember>>() {
  };
  protected static final ParameterizedTypeReference<MMChannel> RESPONSE_TYPE_TEAM_CHANNEL = new ParameterizedTypeReference<MMChannel>() {
  };
  protected static final ParameterizedTypeReference<List<MMChannel>> RESPONSE_TYPE_TEAM_CHANNELS = new ParameterizedTypeReference<List<MMChannel>>() {
  };

  MattermostClient(MattermostClientConfig config, RestTemplateBuilder restTemplateBuilder) {
    super(config, restTemplateBuilder);
    prepareAuthenticationEntity("Authorization", "Bearer " + config.getPersonalAccessToken());
  }

  public List<MMUser> getUsers() {
    LOG.debug("Retrieving users");
    String apiCall = "/users";
    Map<String, String> uriVariables = createUriVariables();
    return makeReadListApiCall(apiCall, HttpMethod.GET, RESPONSE_TYPE_USERS, uriVariables);
  }

  public MMUser getUserById(String userId) {
    if (!StringUtils.hasText(userId)) {
      throw new IllegalStateException("Mattermost user id required");
    }
    LOG.debug("Retrieving user with id '{}'", userId);
    String apiCall = "/users/{userId}";
    Map<String, String> uriVariables = createUriVariables("userId", userId);
    return makeReadApiCall(apiCall, HttpMethod.GET, RESPONSE_TYPE_USER, uriVariables, HttpStatus.NOT_FOUND);
  }

  public MMUser getUserByUsername(String username) {
    if (!StringUtils.hasText(username)) {
      throw new IllegalStateException("Mattermost user name required");
    }
    LOG.debug("Retrieving user with username '{}'", username);
    String apiCall = "/users/username/{username}";
    Map<String, String> uriVariables = createUriVariables("username", username);
    return makeReadApiCall(apiCall, HttpMethod.GET, RESPONSE_TYPE_USER, uriVariables, HttpStatus.NOT_FOUND);
  }

  public List<MMUser> getUsersByUsernames(Collection<String> usernames) {
    if (usernames == null) {
      throw new IllegalStateException("List of Mattermost usernames required");
    }
    if (usernames.isEmpty()) {
      return Collections.emptyList();
    }
    LOG.debug("Retrieving users with usernames {}", usernames);
    String apiCall = "/users/usernames";
    HttpEntity<String> entity = new HttpEntity<String>(new JSONArray(usernames).toString(),
        authenticationEntity.getHeaders());
    Map<String, String> uriVariables = createUriVariables();
    return makeReadListApiCall(apiCall, HttpMethod.POST, entity, RESPONSE_TYPE_USERS, uriVariables);
  }

  public MMTeam getTeamByName(String teamName) {
    if (!StringUtils.hasText(teamName)) {
      throw new IllegalStateException("Mattermost team name required");
    }
    LOG.debug("Retrieving team '{}'", teamName);
    String apiCall = "/teams/name/{teamName}";
    Map<String, String> uriVariables = createUriVariables("teamName", teamName);
    MMTeam team = makeReadApiCall(apiCall, HttpMethod.GET, RESPONSE_TYPE_TEAM, uriVariables);
    if (team != null) {
      team.setUrl(getServerUrl() + "/" + teamName);
    }
    return team;
  }

  public boolean addMemberToTeam(MMTeam team, MMUser user) {
    if (team == null || !StringUtils.hasText(team.getId())) {
      throw new IllegalStateException("Mattermost team with valid ID required");
    }
    if (user == null || !StringUtils.hasText(user.getId())) {
      throw new IllegalStateException("Mattermost user with valid ID required");
    }
    LOG.debug("Adding user '{}' to team '{}'", user.getUsername(), team.getName());
    String apiCall = "/teams/{teamId}/members";
    Map<String, String> uriVariables = createUriVariables("teamId", team.getId(), "userId", user.getId());
    HttpEntity<String> entity = new HttpEntity<String>(
        "{\"team_id\": \"" + team.getId() + "\", \"user_id\": \"" + user.getId() + "\", \"roles\": \"team_user\"}",
        authenticationEntity.getHeaders());
    return makeWriteApiCall(apiCall, HttpMethod.POST, entity, uriVariables);
  }

  public boolean removeMemberFromTeam(MMTeam team, MMUser user) {
    if (team == null || !StringUtils.hasText(team.getId())) {
      throw new IllegalStateException("Mattermost team with valid ID required");
    }
    if (user == null || !StringUtils.hasText(user.getId())) {
      throw new IllegalStateException("Mattermost user with valid ID required");
    }
    LOG.debug("Removing user '{}' from team '{}'", user.getUsername(), team.getName());
    String apiCall = "/teams/{teamId}/members/{userId}";
    Map<String, String> uriVariables = createUriVariables("teamId", team.getId(), "userId", user.getId());
    return makeWriteApiCall(apiCall, HttpMethod.DELETE, uriVariables);
  }

  public List<MMMember> getTeamMembers(MMTeam team) {
    if (team == null || !StringUtils.hasText(team.getId())) {
      throw new IllegalStateException("Mattermost team with valid ID required");
    }
    LOG.debug("Retrieving members for team '{}'", team.getName());
    String apiCall = "/teams/{teamId}/members";
    Map<String, String> uriVariables = createUriVariables("teamId", team.getId());
    return makeReadListApiCall(apiCall, HttpMethod.GET, RESPONSE_TYPE_MEMBERS, uriVariables);
  }

  public MMChannel createChannel(MMTeam team, String name, String displayName, String purpose, String header,
      boolean isPrivate) {
    if (team == null || !StringUtils.hasText(team.getId())) {
      throw new IllegalStateException("Mattermost team with valid ID required");
    }
    if (!StringUtils.hasText(name)) {
      throw new IllegalStateException("name required");
    }
    if (!StringUtils.hasText(displayName)) {
      displayName = name;
    }
    LOG.debug("Creating {} channel: name={}, displayName={}", isPrivate ? "private" : "public", name, displayName);
    String apiCall = "/channels";
    HttpEntity<String> entity = new HttpEntity<String>("{\"team_id\" : \"" + team.getId() + "\", \"name\" : \"" + name
        + "\", \"display_name\" : \"" + displayName + "\", \"purpose\" : \"" + purpose + "\", \"header\" : \"" + header
        + "\", \"type\" : \"" + (isPrivate ? "P" : "O") + "\"}", authenticationEntity.getHeaders());
    return makeWriteApiCall(apiCall, entity, MMChannel.class);
  }

  public MMChannel getChannelByName(MMTeam team, String name) {
    if (team == null || !StringUtils.hasText(team.getId())) {
      throw new IllegalStateException("Mattermost team with valid ID required");
    }
    if (!StringUtils.hasText(name)) {
      throw new IllegalStateException("Mattermost team name required");
    }
    LOG.debug("Retrieving channel '{}' for team '{}'", name, team.getName());
    String apiCall = "/teams/{teamId}/channels/name/{name}?include_deleted=true";
    Map<String, String> uriVariables = createUriVariables("teamId", team.getId(), "name", name);
    return makeReadApiCall(apiCall, HttpMethod.GET, RESPONSE_TYPE_TEAM_CHANNEL, uriVariables, HttpStatus.NOT_FOUND);
  }

  public List<MMChannel> getChannelsByIds(MMTeam team, List<String> channelIds) {
    if (team == null || !StringUtils.hasText(team.getId())) {
      throw new IllegalStateException("Mattermost team with valid ID required");
    }
    if (channelIds == null) {
      throw new IllegalStateException("List of Mattermost channel ids required");
    }
    if (channelIds.isEmpty()) {
      return Collections.emptyList();
    }
    LOG.debug("Retrieving channels by ids for team '{}'", team.getName());
    String apiCall = "/teams/{teamId}/channels/ids";
    Map<String, String> uriVariables = createUriVariables("teamId");
    HttpEntity<String> entity = new HttpEntity<String>(new JSONArray(channelIds).toString(),
        authenticationEntity.getHeaders());
    return makeReadListApiCall(apiCall, HttpMethod.POST, entity, RESPONSE_TYPE_TEAM_CHANNELS, uriVariables);
  }

  public boolean updateChannel(MMChannel channel, String name, String displayName, String purpose, String header) {
    if (channel == null || !StringUtils.hasText(channel.getId())) {
      throw new IllegalStateException("Mattermost channel with valid ID required");
    }
    if (!StringUtils.hasText(name)) {
      throw new IllegalStateException("name required");
    }
    if (!StringUtils.hasText(displayName)) {
      displayName = name;
    }
    LOG.debug("Updating channel {}: name={}, displayName={}", channel.getId(), name, displayName);
    String apiCall = "/channels/{channelId}";
    Map<String, String> uriVariables = createUriVariables("channelId", channel.getId());
    HttpEntity<String> entity = new HttpEntity<String>(
        "{\"id\" : \"" + channel.getId() + "\", \"name\" : \"" + name + "\", \"display_name\" : \"" + displayName
            + "\", \"purpose\" : \"" + purpose + "\", \"header\" : \"" + header + "\"}",
        authenticationEntity.getHeaders());
    return makeWriteApiCall(apiCall, HttpMethod.PUT, entity, uriVariables);
  }

  public boolean convertChannelIntoPrivate(MMChannel channel) {
    if (channel == null || !StringUtils.hasText(channel.getId())) {
      throw new IllegalStateException("Mattermost channel with valid ID required");
    }
    LOG.debug("Converting channel '{}'", channel.getName());
    String apiCall = "/channels/{channelId}/convert";
    Map<String, String> uriVariables = createUriVariables("channelId", channel.getId());
    return makeWriteApiCall(apiCall, HttpMethod.POST, uriVariables);
  }

  public boolean deleteChannel(MMChannel channel) {
    if (channel == null || !StringUtils.hasText(channel.getId())) {
      throw new IllegalStateException("Mattermost channel with valid ID required");
    }
    LOG.debug("Deleting channel '{}'", channel.getName());
    String apiCall = "/channels/{channelId}";
    Map<String, String> uriVariables = createUriVariables("channelId", channel.getId());
    return makeWriteApiCall(apiCall, HttpMethod.DELETE, uriVariables);
  }

  public MMChannel restoreChannel(MMChannel channel) {
    if (channel == null || !StringUtils.hasText(channel.getId())) {
      throw new IllegalStateException("Mattermost channel with valid ID required");
    }
    LOG.debug("Restoring channel '{}'", channel.getName());
    String apiCall = "/channels/{channelId}/restore";
    Map<String, String> uriVariables = createUriVariables("channelId", channel.getId());
    return makeReadApiCall(apiCall, HttpMethod.POST, RESPONSE_TYPE_TEAM_CHANNEL, uriVariables);
  }

  public boolean addMemberToChannel(MMChannel channel, MMUser user) {
    if (channel == null || !StringUtils.hasText(channel.getId())) {
      throw new IllegalStateException("Mattermost team with valid ID required");
    }
    if (user == null || !StringUtils.hasText(user.getId())) {
      throw new IllegalStateException("Mattermost user with valid ID required");
    }
    LOG.debug("Adding user '{}' to channel '{}'", user.getUsername(), channel.getName());
    String apiCall = "/channels/{channelId}/members";
    Map<String, String> uriVariables = createUriVariables("channelId", channel.getId());
    HttpEntity<String> entity = new HttpEntity<String>("{\"user_id\" : \"" + user.getId() + "\"}",
        authenticationEntity.getHeaders());
    return makeWriteApiCall(apiCall, HttpMethod.POST, entity, uriVariables);
  }

  public boolean removeMemberFromChannel(MMChannel channel, String userId) {
    if (channel == null || !StringUtils.hasText(channel.getId())) {
      throw new IllegalStateException("Mattermost team with valid ID required");
    }
    if (!StringUtils.hasText(userId)) {
      throw new IllegalStateException("Mattermost user with valid ID required");
    }
    LOG.debug("Removing user '{}' from channel '{}'", userId, channel.getName());
    String apiCall = "/channels/{channelId}/members/{userId}";
    Map<String, String> uriVariables = createUriVariables("channelId", channel.getId(), "userId", userId);
    return makeWriteApiCall(apiCall, HttpMethod.DELETE, uriVariables);
  }

  public List<MMMember> getChannelMembers(MMChannel channel) {
    if (channel == null || !StringUtils.hasText(channel.getId())) {
      throw new IllegalStateException("Mattermost team channel valid ID required");
    }
    LOG.debug("Retrieving members for channel '{}'", channel.getName());
    String apiCall = "/channels/{channelId}/members";
    Map<String, String> uriVariables = createUriVariables("channelId", channel.getId());
    return makeReadListApiCall(apiCall, HttpMethod.GET, RESPONSE_TYPE_MEMBERS, uriVariables);
  }

  @Override
  protected <T> List<T> makeReadListApiCall(String apiCall, HttpMethod method,
      ParameterizedTypeReference<List<T>> typeReference, Map<String, String> uriVariables, HttpStatus... ignoreStatus) {
    int page = 0;
    String url = getApiUrl(apiCall + (apiCall.contains("?") ? "&" : "?") + "page={page}&per_page={perPage}");
    uriVariables.put("page", Integer.toString(page));
    uriVariables.put("perPage", perPageAsString());
    try {
      List<T> entities;
      ResponseEntity<List<T>> response = restTemplate.exchange(url, method, authenticationEntity, typeReference,
          uriVariables);
      if (response.getBody().size() < config.getPerPage()) {
        entities = response.getBody();
      } else {
        entities = new ArrayList<>(response.getBody());
        do {
          page++;
          uriVariables.put("page", Integer.toString(page));
          response = restTemplate.exchange(url, method, authenticationEntity, typeReference, uriVariables);
          entities.addAll(response.getBody());
        } while (response.getBody().size() == config.getPerPage());
        return entities;
      }
      return entities;
    } catch (RestClientException e) {
      logException(method, uriVariables, url, e, ignoreStatus);
    }
    return null;
  }

  protected <T> List<T> makeReadListApiCall(String apiCall, HttpMethod method, HttpEntity<String> entity,
      ParameterizedTypeReference<List<T>> typeReference, Map<String, String> uriVariables, HttpStatus... ignoreStatus) {
    int page = 0;
    String url = getApiUrl(apiCall + (apiCall.contains("?") ? "&" : "?") + "page={page}&per_page={perPage}");
    uriVariables.put("page", Integer.toString(page));
    uriVariables.put("perPage", perPageAsString());
    try {
      List<T> entities;
      ResponseEntity<List<T>> response = restTemplate.exchange(url, method, entity, typeReference, uriVariables);
      if (response.getBody().size() < config.getPerPage()) {
        entities = response.getBody();
      } else {
        entities = new ArrayList<>(response.getBody());
        do {
          page++;
          uriVariables.put("page", Integer.toString(page));
          response = restTemplate.exchange(url, method, authenticationEntity, typeReference, uriVariables);
          entities.addAll(response.getBody());
        } while (response.getBody().size() == config.getPerPage());
        return entities;
      }
      return entities;
    } catch (RestClientException e) {
      logException(method, uriVariables, url, e, ignoreStatus);
    }
    return null;
  }
}
