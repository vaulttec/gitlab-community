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
package org.vaulttec.gitlab.community.model;

import java.net.URI;
import java.net.URL;

import org.springframework.util.StringUtils;
import org.vaulttec.gitlab.community.CommunityConfig;
import org.vaulttec.gitlab.community.gitlab.model.GLGroup;
import org.vaulttec.gitlab.community.mattermost.model.MMTeam;

public class Community {
  private final GLGroup group;
  private final MMTeam team;
  private final CommunityConfig config;

  public Community(CommunityConfig config, GLGroup group, MMTeam team) {
    this.config = config;
    this.group = group;
    this.team = team;
  }

  public String getId() {
    return group.getId();
  }

  public String getPath() {
    return group.getPath();
  }

  public String getName() {
    return group.getName();
  }

  public String getDescription() {
    return group.getDescription();
  }

  public URL getAvatar() {
    return group.getAvatar();
  }

  public URL getProfile() {
    return group.getProfile();
  }

  public MMTeam getTeam() {
    return team;
  }

  public String getSpectatorModeMessage() {
    return StringUtils.isEmpty(config.getSpectatorModeMessage()) ? null
        : StringUtils.replace(config.getSpectatorModeMessage(), "{0}", getPath());
  }

  public URI getOnlineHelpUri() {
    return config.getOnlineHelpUri();
  }

  public URI getNewIssueUri() {
    return config.getNewIssueUri();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((group == null) ? 0 : group.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Community other = (Community) obj;
    if (group == null) {
      if (other.group != null)
        return false;
    } else if (!group.equals(other.group))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "Community [group=" + group + ", team=" + team + "]";
  }
}
