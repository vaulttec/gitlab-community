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
package org.vaulttec.gitlab.community.mattermost.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MMChannel {
  private String id;
  private String name;
  @JsonProperty("display_name")
  private String displayName;
  private String purpose;
  private String header;
  @JsonProperty("type")
  boolean isPrivate;
  @JsonProperty("total_msg_count")
  int messageCount;
  @JsonProperty("create_at")
  private Date createAt;
  @JsonProperty("delete_at")
  private Date deleteAt;
  @JsonProperty("last_post_at")
  private Date lastPostAt;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getPurpose() {
    return purpose;
  }

  public void setPurpose(String purpose) {
    this.purpose = purpose;
  }

  public String getHeader() {
    return header;
  }

  public void setHeader(String header) {
    this.header = header;
  }

  public boolean isPrivate() {
    return isPrivate;
  }

  @JsonSetter("type")
  public void setPrivate(String type) {
    this.isPrivate = "P".equals(type);
  }

  public int getMessageCount() {
    return messageCount;
  }

  public void setMessageCount(int messageCount) {
    this.messageCount = messageCount;
  }

  public Date getCreateAt() {
    return createAt;
  }

  public void setCreateAt(Date createAt) {
    this.createAt = createAt;
  }

  public Date getDeleteAt() {
    return deleteAt;
  }

  public void setDeleteAt(Date deleteAt) {
    this.deleteAt = deleteAt;
  }

  public Date getLastPostAt() {
    return lastPostAt;
  }

  public void setLastPostAt(Date lastPostAt) {
    this.lastPostAt = lastPostAt;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
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
    MMChannel other = (MMChannel) obj;
    if (id == null) {
      if (other.id != null) {
        return false;
      }
    } else if (!id.equals(other.id)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "MMChannel [id=" + id + ", name=" + name + ", displayName=" + displayName + ", purpose=" + purpose
        + ", header=" + header + ", isPrivate=" + isPrivate + ", messageCount=" + messageCount + ", createAt="
        + createAt + ", deleteAt=" + deleteAt + ", lastPostAt=" + lastPostAt + "]";
  }
}
