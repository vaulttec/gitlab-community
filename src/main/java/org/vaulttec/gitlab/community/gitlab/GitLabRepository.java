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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import org.vaulttec.gitlab.community.gitlab.model.GLUser;

@Repository
public class GitLabRepository {
  private static final Logger LOG = LoggerFactory.getLogger(GitLabRepository.class);

  private final GitLabClient gitLabClient;

  public GitLabRepository(GitLabClient gitLabClient) {
    LOG.debug("Initializing GitLabRepository");
    this.gitLabClient = gitLabClient;
  }

  @Cacheable("gl_users")
  public Map<String, GLUser> getUsers() {
    return retrieveUsers();
  }

  @CachePut("gl_users")
  public Map<String, GLUser> refreshUsers() {
    LOG.debug("Refreshing users");
    return retrieveUsers();
  }

  private Map<String, GLUser> retrieveUsers() {
    LOG.debug("Retrieving all users");
    Map<String, GLUser> allUsers = new HashMap<String, GLUser>();
    List<GLUser> users = gitLabClient.getActiveUsers();
    if (users != null) {
      users.forEach(user -> allUsers.put(user.getUsername(), user));
    }
    return allUsers;
  }
}
