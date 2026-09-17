package com.exchange.mod.core;

/**
 * Model representing a financial candlestick (OHLCV).
 */
public class Candle {
    private final long timestamp;
    private final double open;
    private double high;
    private double low;
    private double close;
    private double volume;

    public Candle(long timestamp, double open, double high, double low, double close, double volume) {
        this.timestamp = timestamp;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }

    public void update(double price, double tradeVolume) {
        if (price > this.high) this.high = price;
        if (price < this.low) this.low = price;
        this.close = price;
        this.volume += tradeVolume;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getOpen() {
        return open;
    }

    public double getHigh() {
        return high;
    }

    public double getLow() {
        return low;
    }

    public double getClose() {
        return close;
    }

    public double getVolume() {
        return volume;
    }

    public boolean isBullish() {
        return close >= open;
    }
}
