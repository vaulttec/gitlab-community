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
import java.time.LocalDate;

import org.vaulttec.gitlab.community.gitlab.model.GLUser;

public class Member {
  private String username;
  private String name;
  private String email;
  private String userId;
  private URL avatar;
  private URL profile;
  private LocalDate joined;
  private boolean isAdmin;

  public Member(GLUser user, boolean isAdmin) {
    this.userId = user.getId();
    this.username = user.getUsername();
    this.name = user.getName();
    this.email = user.getEmail();
    this.avatar = user.getAvatar();
    this.profile = user.getProfile();
    this.joined = user.getJoined();
    this.isAdmin = isAdmin;
  }

  public String getUserId() {
    return userId;
  }

  public String getUsername() {
    return username;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public URL getAvatar() {
    return avatar;
  }

  public URL getProfile() {
    return profile;
  }

  public LocalDate getJoined() {
    return joined;
  }

  public boolean isAdmin() {
    return isAdmin;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((username == null) ? 0 : username.hashCode());
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
    Member other = (Member) obj;
    if (username == null) {
      if (other.username != null) {
        return false;
      }
    } else if (!username.equals(other.username)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "Member [username=" + username + ", name=" + name + ", isAdmin=" + isAdmin + "]";
  }
}
