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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import org.vaulttec.gitlab.community.mattermost.model.MMUser;

@Repository
public class MattermostRepository {
  private static final Logger LOG = LoggerFactory.getLogger(MattermostRepository.class);

  private final MattermostClient mattermostClient;

  public MattermostRepository(MattermostClient mattermostClient) {
    LOG.debug("Initializing MattermostRepository");
    this.mattermostClient = mattermostClient;
  }

  @Cacheable("mm_users")
  public Map<String, MMUser> getUsers() {
    return retrieveUsers();
  }

  @CachePut("mm_users")
  public Map<String, MMUser> refreshUsers() {
    LOG.debug("Refreshing users");
    return retrieveUsers();
  }

  private Map<String, MMUser> retrieveUsers() {
    LOG.debug("Retrieving all users");
    Map<String, MMUser> allUsers = new HashMap<String, MMUser>();
    List<MMUser> users = mattermostClient.getUsers();
    if (users != null) {
      users.forEach(user -> allUsers.put(user.getId(), user));
    }
    return allUsers;
  }
}
