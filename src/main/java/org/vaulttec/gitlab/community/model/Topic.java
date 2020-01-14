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

import java.net.URL;
import java.util.Date;

import javax.validation.constraints.NotBlank;

import org.vaulttec.gitlab.community.gitlab.model.GLGroup;
import org.vaulttec.gitlab.community.mattermost.model.MMChannel;

public class Topic {
  @NotBlank(message = "Path is mandatory")
  private String path;
  @NotBlank(message = "Name is mandatory")
  private String name;
  @NotBlank(message = "Description is mandatory")
  private String description;
  private String groupId;
  private URL avatar;
  private URL profile;
  private String channelId;
  private int messageCount;
  private Date createAt;
  private Date lastPostAt;

  public Topic() {
  }

  public Topic(GLGroup group, MMChannel channel) {
    this.path = group.getPath();
    this.name = group.getName();
    this.description = group.getDescription();
    this.groupId = group.getId();
    this.avatar = group.getAvatar();
    this.profile = group.getProfile();
    if (channel != null) {
      this.channelId = channel.getId();
      this.messageCount = channel.getMessageCount();
      this.createAt = channel.getCreateAt();
      this.lastPostAt = channel.getLastPostAt();
    }
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getGroupId() {
    return groupId;
  }

  public URL getAvatar() {
    return avatar;
  }

  public URL getProfile() {
    return profile;
  }

  public String getChannelId() {
    return channelId;
  }

  public int getMessageCount() {
    return messageCount;
  }

  public Date getCreateAt() {
    return createAt;
  }

  public Date getLastPostAt() {
    return lastPostAt;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((path == null) ? 0 : path.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Topic other = (Topic) obj;
    if (path == null) {
      if (other.path != null) {
        return false;
      }
    } else if (!path.equals(other.path)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "Topic [path=" + path + ", name=" + name + ", description=" + description + ", groupId=" + groupId
        + ", channelId=" + channelId + ", messageCount=" + messageCount + ", createAt=" + createAt + ", lastPostAt="
        + lastPostAt + "]";
  }
}
