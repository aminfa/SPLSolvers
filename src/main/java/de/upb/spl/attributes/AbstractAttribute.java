package de.upb.spl.attributes;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.Objects;

public abstract class AbstractAttribute {
	private final String attributeName;
	private final String aggregationMethod;
	private final boolean toBeMinimized;

	public AbstractAttribute(String attributeName, String aggregationMethod, boolean toBeMinimized) {
		this.attributeName = Objects.requireNonNull(attributeName);
        this.aggregationMethod = aggregationMethod;
        this.toBeMinimized = toBeMinimized;
    }

	public String name() {
		return attributeName;
	}

	public boolean isToBeMinimized() {
	    return toBeMinimized;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof AbstractAttribute)) return false;

        AbstractAttribute attribute = (AbstractAttribute) o;

        return new EqualsBuilder()
                .append(attributeName, attribute.attributeName)
                .append(aggregationMethod, attribute.aggregationMethod)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(attributeName)
                .append(aggregationMethod)
                .toHashCode();
    }

    public abstract double value(double rand);


    public static AbstractAttribute createFromConfig(AttributeConfiguration configuration) {
        return createFromConfig(configuration, configuration.getAttributeName());
    }

    public static AbstractAttribute createFromConfig(AttributeConfiguration configuration, String attributeName) {
	    String aggregationMethod = configuration.getAggregationMethod();
	    boolean toBeMinimized = configuration.toBeMinimized();
	    if(configuration.getDistributionType().equals("Gaussian")) {
            return new GaussianDistAttribute(attributeName, aggregationMethod, toBeMinimized, configuration.getMean(), configuration.getDeviation());
        } else if(configuration.getDistributionType().equals("Uniform")) {
            return new UniformAttribute(attributeName, aggregationMethod, toBeMinimized, configuration.getRangeStart(), configuration.getRangeEnd());
        } else if(configuration.getDistributionType().equals("Bernoulli")) {
            return new BernoulliAttribute(attributeName, aggregationMethod, toBeMinimized, configuration.getBernoulliP());
        } else {
	        throw new IllegalArgumentException("Distribution type is not known: " + configuration.getDistributionType());
        }
    }

    public String getAggregationMethod() {
	    return aggregationMethod;
    }

    static class SimpleAttribute extends AbstractAttribute{

		public SimpleAttribute(String attributeName, String aggregationMethod, boolean toBeMinimized) {
			super(attributeName, aggregationMethod, toBeMinimized);
		}

		@Override
		public double value(double rand) {
			return rand;
		}
	}

	static class UniformAttribute extends AbstractAttribute{

	    private final double start, end, size;
        public UniformAttribute(String attributeName, String aggregationMethod, boolean toBeMinimized, double start, double end) {
            super(attributeName, aggregationMethod, toBeMinimized);
            if(end >= start) {
                this.start = start;
                this.end = end;
            } else {
                this.start = end;
                this.end = start;
            }
            this.size = end - start;
        }

        @Override
        public double value(double rand) {
            return start + (size * rand);
        }
    }

    static class GaussianDistAttribute extends AbstractAttribute{

        private final double mean, dev;
        private final NormalDistribution distribution;

        public GaussianDistAttribute(String attributeName, String aggregationMethod, boolean toBeMinimized, double mean, double dev) {
            super(attributeName, aggregationMethod, toBeMinimized);
            this.mean = mean;
            this.dev = dev;
            this.distribution = new NormalDistribution();
        }

        @Override
        public double value(double rand) {
            rand = (rand * 15/16d) + 1/32d; // getting rid of edge percentile by shrinking 0 - 1 to 1/32 - 31/32.
            double val = distribution.inverseCumulativeProbability(rand);
            val = (val * dev) + mean;
            return val;
        }
    }
    static class BernoulliAttribute extends AbstractAttribute{

	    private final double p;

        public BernoulliAttribute(String attributeName, String aggregationMethod, boolean toBeMinimized, double p) {
            super(attributeName, aggregationMethod, toBeMinimized);
            this.p = p;
        }

        @Override
        public double value(double rand) {
            return  rand < p ? 1. : 0.;
        }
    }
}
