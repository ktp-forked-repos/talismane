///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Joliciel Informatique
//
//This file is part of Talismane.
//
//Talismane is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Talismane is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Talismane.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.talismane.filters;

import com.joliciel.talismane.tokeniser.TokenAttribute;

class TextMarkerImpl implements TextMarker {
	private TextMarkerType type;
	private int position;
	private String insertionText;
	private TextMarkerFilter source;
	private String matchText;
	private String attribute;
	private TokenAttribute<?> value;

	public TextMarkerImpl(TextMarkerType type, int position) {
		super();
		this.type = type;
		this.position = position;
	}

	@Override
	public TextMarkerType getType() {
		return type;
	}

	@Override
	public int getPosition() {
		return position;
	}

	@Override
	public void setPosition(int position) {
		this.position = position;
	}

	@Override
	public int compareTo(TextMarker o) {
		if (this == o)
			return 0;
		if (this.position != o.getPosition())
			return this.position - o.getPosition();
		if (!this.getType().equals(o.getType()))
			return this.getType().compareTo(o.getType());
		return 1;
	}

	@Override
	public String toString() {
		return "TextMarkerImpl [type=" + type + ", position=" + position + "]";
	}

	@Override
	public String getInsertionText() {
		return insertionText;
	}

	@Override
	public void setInsertionText(String insertionText) {
		this.insertionText = insertionText;
	}

	@Override
	public TextMarkerFilter getSource() {
		return source;
	}

	@Override
	public void setSource(TextMarkerFilter source) {
		this.source = source;
	}

	@Override
	public String getMatchText() {
		return matchText;
	}

	@Override
	public void setMatchText(String matchText) {
		this.matchText = matchText;
	}

	@Override
	public <T> void setTag(String attribute, TokenAttribute<T> value) {
		this.attribute = attribute;
		this.value = value;
	}

	@Override
	public String getAttribute() {
		return attribute;
	}

	@Override
	public TokenAttribute<?> getValue() {
		return value;
	}
}