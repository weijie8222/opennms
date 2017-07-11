/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2017-2017 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2017 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.web.rest.v2;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opennms.core.config.api.JaxbListWrapper;
import org.opennms.core.test.rest.AbstractSpringJerseyRestTestCase;
import org.opennms.netmgt.model.OnmsAlarm;
import org.opennms.web.api.Authentication;
import org.springframework.mock.web.MockHttpServletRequest;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * A REST client for the v2 alarm API that uses
 * JSON as the primary content type.
 *
 * @author jwhite
 */
public class AlarmRestV2Client {

    private final AbstractSpringJerseyRestTestCase testCase;

    public AlarmRestV2Client(AbstractSpringJerseyRestTestCase testCase) {
        this.testCase = Objects.requireNonNull(testCase);
    }

    public JaxbListWrapper<OnmsAlarm> getAlarms(String query) {
        try {
            String url = "/alarms";
            Map<String, String> parms = AbstractSpringJerseyRestTestCase.parseParamData(query);

            MockHttpServletRequest jsonRequest = testCase.createRequest(AbstractSpringJerseyRestTestCase.GET, url, "admin", Arrays.asList(new String[]{ Authentication.ROLE_ADMIN }));
            jsonRequest.setParameters(parms);
            jsonRequest.setQueryString(AbstractSpringJerseyRestTestCase.getQueryString(parms));
            jsonRequest.addHeader("Accept", MediaType.APPLICATION_JSON);
            String json = testCase.sendRequest(jsonRequest, 200);

            return alarmListWrapperFromJson(json);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public OnmsAlarm getAlarm(int alarmId) {
        // Leverage the query functionality to lookup specific elements instead
        // of passing the id in the URL
        final JaxbListWrapper<OnmsAlarm> alarms = getAlarms(String.format("_s=alarm.id==%d", alarmId));
        assertEquals(1, alarms.size());
        return alarms.get(0);
    }

    public void setOrUpdateSticky(int alarmId, String memo) {
        setOrUpdateMemo(alarmId, MemoType.STICKY, memo);
    }

    public void setOrUpdateJournal(int alarmId, String memo) {
        setOrUpdateMemo(alarmId, MemoType.JOURNAL, memo);
    }

    private void setOrUpdateMemo(int alarmId, MemoType type, String body) {
        final String url = String.format("/alarms/%d/%s", alarmId, type.getCode());
        final Map<String, String> parms = Maps.newHashMap();
        parms.put("body", body);
        parms.put("user", "admin");
        final String formData = AbstractSpringJerseyRestTestCase.getQueryString(parms);
        try {
            testCase.sendPut(url, formData, 204, null);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public void deleteSticky(int alarmId) {
        deleteMemo(alarmId, MemoType.STICKY);
    }

    public void deleteJournal(int alarmId) {
        deleteMemo(alarmId, MemoType.JOURNAL);
    }

    private void deleteMemo(int alarmId, MemoType type) {
        final String url = String.format("/alarms/%d/%s", alarmId, type.getCode());
        try {
            testCase.sendData(AbstractSpringJerseyRestTestCase.DELETE, MediaType.APPLICATION_FORM_URLENCODED, url, "", 204);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private static JaxbListWrapper<OnmsAlarm> alarmListWrapperFromJson(String json) throws JSONException, JsonParseException, JsonMappingException, IOException {
        // Build the mapper
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospector introspector = new JaxbAnnotationIntrospector();
        mapper.setAnnotationIntrospector(introspector);
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Retrieve the alarm array from the JSON string, since
        // mapping these directly in the list wrapper fails
        JSONObject jsonObject = new JSONObject(json);
        JSONArray jsonAlarms = jsonObject.getJSONArray("alarm");
        OnmsAlarm[] alarms = mapper.readValue(jsonAlarms.toString(), OnmsAlarm[].class);

        // Use the mapper to populate the count, totalCount and offset fields of the list wrapper
        @SuppressWarnings("unchecked")
        JaxbListWrapper<OnmsAlarm> listWrapper = mapper.readValue(json, JaxbListWrapper.class);
        // Use the array we parsed previously
        listWrapper.setObjects(Lists.newArrayList(alarms));
        return listWrapper;
    }

    private static enum MemoType {
        STICKY("memo"),
        JOURNAL("journal");

        private final String code;

        MemoType(String code) {
            this.code = Objects.requireNonNull(code);
        }

        public String getCode() {
            return code;
        }
    }

}
