package ca.uhn.fhir.rest.gclient;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ca.uhn.fhir.rest.param.ParameterUtil;

/*
 * #%L
 * HAPI FHIR - Core Library
 * %%
 * Copyright (C) 2014 - 2015 University Health Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

class StringCriterion<A extends IParam> implements ICriterion<A>, ICriterionInternal {

	private String myValue;
	private String myName;

	public StringCriterion(String theName, String theValue) {
		myName=theName;
		myValue = ParameterUtil.escape(theValue);
	}

	public StringCriterion(String theName, List<String> theValue) {
		myName=theName;
		StringBuilder b = new StringBuilder();
		for (String next : theValue) {
			if (StringUtils.isBlank(next)) {
				continue;
			}
			if (b.length() > 0) {
				b.append(',');
			}
			b.append(ParameterUtil.escape(next));
		}
		myValue = b.toString();
	}

	@Override
	public String getParameterName() {
		return myName;
	}

	@Override
	public String getParameterValue() {
		return myValue;
	}

}