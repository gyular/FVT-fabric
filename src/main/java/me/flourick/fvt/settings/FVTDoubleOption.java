package me.flourick.fvt.settings;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import me.flourick.fvt.FVT;

import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.options.GameOptions;
import net.minecraft.text.LiteralText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

public class FVTDoubleOption extends FVTOption<Double>
{
	public enum Mode {
		NORMAL,
		WHOLE, 
		PERCENT
	}

	private double currentValue;
	private double defaultValue;

	private double min;
	private double max;
	private double step;

	private Mode mode;

	private Text tooltipText;

	public FVTDoubleOption(String key, String tooltipKey, double min, double max, double step, double defaultValue)
	{
		super(key);
		this.defaultValue = defaultValue;
		this.min = min;
		this.max = max;
		this.step = step;
		this.currentValue = getRatio(defaultValue);
		this.mode = Mode.NORMAL;
		this.tooltipText = new TranslatableText(tooltipKey);
	}

	public FVTDoubleOption(String key, String tooltipKey, double min, double max, double step, double defaultValue, Mode mode)
	{
		super(key);
		this.defaultValue = defaultValue;
		this.min = min;
		this.max = max;
		this.step = step;
		this.currentValue = getRatio(defaultValue);
		this.mode = mode;
		this.tooltipText = new TranslatableText(tooltipKey);
	}

	@Override
	public AbstractButtonWidget createButton(GameOptions options, int x, int y, int width)
	{
		List<OrderedText> tooltip = new ArrayList<OrderedText>();
		tooltip.addAll(FVT.MC.textRenderer.wrapLines(tooltipText, 220));
		tooltip.add(new TranslatableText("fvt.feature.default", getDisplayDefaultValue()).formatted(Formatting.GRAY).asOrderedText());
		setTooltip(tooltip);

		return new FVTDoubleOptionSliderWidget(options, x, y, width, 20, currentValue, this);
	}

	public int getValueAsInteger()
	{
		return BigDecimal.valueOf(getValueRaw()).setScale(0, RoundingMode.DOWN).intValue();
	}

	@Override
	public String getValueAsString()
	{
		return BigDecimal.valueOf(getValueRaw()).setScale(2, RoundingMode.HALF_UP).toString();
	}

	@Override
	public boolean setValueFromString(String newValue)
	{
		boolean ret = true;

		try {
			currentValue = getRatio(Double.parseDouble(newValue));
		}
		catch(NumberFormatException e) {
			ret = false;
		}

		return ret;
	}

	@Override
	public Double getValueRaw()
	{
		return adjust(MathHelper.lerp(MathHelper.clamp(currentValue, 0.0d, 1.0d), min, max));
	}

	public Double getValueRawNormalized()
	{
		return currentValue;
	}

	protected void setValue(double newValue)
	{
		currentValue = newValue;
	}

	protected double getRatio(double value)
	{
		return MathHelper.clamp((adjust(value) - min) / (max - min), 0.0d, 1.0d);
	}
  
	private double adjust(double value) {
		if(step > 0.0d) {
			value = step * (double)Math.round(value / step);
		}

		return MathHelper.clamp(value, min, max);
	}

	protected Text getButtonLabel()
	{
		if(mode == Mode.WHOLE) {
			return getGenericLabel(new LiteralText(String.valueOf(getValueAsInteger())));
		}
		else if(mode == Mode.PERCENT) {
			return getPercentLabel(getValueRaw());
		}
		else {
			return getGenericLabel(new LiteralText(getValueAsString()));
		}
	}

	private String getDisplayDefaultValue()
	{
		if(mode == Mode.WHOLE) {
			return String.valueOf(BigDecimal.valueOf(defaultValue).setScale(0, RoundingMode.DOWN).intValue());
		}
		else if(mode == Mode.PERCENT) {
			return BigDecimal.valueOf(defaultValue * 100).setScale(0, RoundingMode.DOWN).intValue() + "%";
		}
		else {
			return BigDecimal.valueOf(defaultValue).setScale(2, RoundingMode.HALF_UP).toString();
		}
	}
}