package de.upb.spl.attributes;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public final class FeatureAttribute {
	private final String feature;
	private final AbstractAttribute attribute;

	public FeatureAttribute(String feature, AbstractAttribute attribute) {
		this.feature = feature;
		this.attribute = attribute;
	}

	public final String featureName() {
		return getFeature();
	}

	public final String attributeName() {
		return getAttribute().name();
	}

	public final String getFeature() {
		return feature;
	}

	public final AbstractAttribute getAttribute() {
		return attribute;
	}



	@Override
	public boolean equals(Object o) {
		if (this == o) return true;

		if (!(o instanceof FeatureAttribute)) return false;

		FeatureAttribute that = (FeatureAttribute) o;

		return new EqualsBuilder()
				.append(feature, that.feature)
				.append(attribute, that.attribute)
				.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37)
				.append(feature)
				.append(attribute)
				.toHashCode();
	}

}
