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

import java.net.URI;
import java.util.List;

import javax.validation.constraints.NotEmpty;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.vaulttec.gitlab.community.gitlab.model.GLPermission;

@Configuration
@ConfigurationProperties(prefix = "community")
public class CommunityConfig {
  @NotEmpty
  private String groupPath;
  private GLPermission topicPermission;
  private List<String> adminUsernames;
  private List<String> excludedUsernames;
  private String spectatorModeMessage;
  private URI onlineHelpUri;

  public String getGroupPath() {
    return groupPath;
  }

  public void setGroupPath(String groupPath) {
    this.groupPath = groupPath;
  }

  public List<String> getAdminUsernames() {
    return adminUsernames;
  }

  public void setAdminUsernames(List<String> adminUsernames) {
    this.adminUsernames = adminUsernames;
  }

  public List<String> getExcludedUsernames() {
    return excludedUsernames;
  }

  public void setExcludedUsernames(List<String> excludedUsernames) {
    this.excludedUsernames = excludedUsernames;
  }

  public GLPermission getTopicPermission() {
    return topicPermission;
  }

  public void setTopicPermission(String topicPermission) {
    this.topicPermission = GLPermission.fromName(topicPermission);
  }

  public String getSpectatorModeMessage() {
    return spectatorModeMessage;
  }

  public void setSpectatorModeMessage(String spectatorModeMessage) {
    this.spectatorModeMessage = spectatorModeMessage;
  }

  public URI getOnlineHelpUri() {
    return onlineHelpUri;
  }

  public void setOnlineHelpUri(URI onlineHelpUri) {
    this.onlineHelpUri = onlineHelpUri;
  }

  @Override
  public String toString() {
    return "CommunityConfig [groupPath=" + groupPath + ", topicPermission=" + topicPermission + ", adminUsernames="
        + adminUsernames + ", excludedUsernames=" + excludedUsernames + ", spectatorModeMessage=" + spectatorModeMessage
        + ", onlineHelpUri=" + onlineHelpUri + "]";
  }
}
