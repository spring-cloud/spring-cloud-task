/*
 *  Copyright 2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.task.launcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Glenn Renfro
 */
public class TaskLaunchRequestTests {
	public static final String URI = "http://myURI";

	@Test
	public void testEquals() {
		List args = new ArrayList<String>();
		Map map = new HashMap<String,String>();
		args.add("foo");
		map.put("bar", "baz");

		TaskLaunchRequest request = new TaskLaunchRequest(URI,
				Collections.EMPTY_LIST,
				Collections.EMPTY_MAP,
				Collections.EMPTY_MAP);
		TaskLaunchRequest request2 = new TaskLaunchRequest(URI,
				Collections.EMPTY_LIST,
				Collections.EMPTY_MAP,
				Collections.EMPTY_MAP);
		assertFalse(request.equals(null));
		assertTrue(request.equals(request));
		assertTrue(request.equals(request2));
		TaskLaunchRequest requestDiff = new TaskLaunchRequest("http://oops",
				Collections.EMPTY_LIST,
				Collections.EMPTY_MAP,
				Collections.EMPTY_MAP);
		assertFalse(request.equals(requestDiff));

		requestDiff = new TaskLaunchRequest(URI,
				args,
				Collections.EMPTY_MAP,
				Collections.EMPTY_MAP);
		assertFalse(request.equals(requestDiff));

		requestDiff = new TaskLaunchRequest(URI,
				Collections.EMPTY_LIST,
				map,
				Collections.EMPTY_MAP);
		assertFalse(request.equals(requestDiff));

		requestDiff = new TaskLaunchRequest(URI,
				Collections.EMPTY_LIST,
				Collections.EMPTY_MAP,
				map);
		assertFalse(request.equals(requestDiff));

	}
}
