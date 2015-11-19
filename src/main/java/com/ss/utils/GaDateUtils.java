package com.ss.utils;

import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

public class GaDateUtils {
	private final static String DATE_FORMAT = "yyyy-MM-dd";
	private final static String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
	private final static String MONTH_FORMAT = "yyyy-MM";

	public final static String WEEK = "week";

	public final static String DAY = "day";

	public final static String MONTH = "month";

	/**
	 * Value of the {@link #DAY_OF_WEEK} field indicating Monday.
	 */
	public final static int MONDAY = 1;

	/**
	 * Value of the {@link #DAY_OF_WEEK} field indicating Tuesday.
	 */
	public final static int TUESDAY = 2;

	/**
	 * Value of the {@link #DAY_OF_WEEK} field indicating Wednesday.
	 */
	public final static int WEDNESDAY = 3;

	/**
	 * Value of the {@link #DAY_OF_WEEK} field indicating Thursday.
	 */
	public final static int THURSDAY = 4;

	/**
	 * Value of the {@link #DAY_OF_WEEK} field indicating Friday.
	 */
	public final static int FRIDAY = 5;

	/**
	 * Value of the {@link #DAY_OF_WEEK} field indicating Saturday.
	 */
	public final static int SATURDAY = 6;
	/**
	 * Value of the {@link #DAY_OF_WEEK} field indicating Sunday
	 */
	public final static int SUNDAY = 7;

	/**
	 * @Title:getMonthFirstDay
	 * @Description: 得到当前月的第一天.
	 * @return
	 * @return String
	 */
	public static String getMonthFirstDay() {
		Calendar cal = Calendar.getInstance();

		cal.set(Calendar.DATE, 1);
		return DateFormatUtils.format(cal, DATE_FORMAT);

	}
	
	/**
	 * @Title:getCurrentMonth
	 * @Description: 得到当前月
	 * @return
	 * @return String
	 */
	public static String getCurrentMonth() {
		Calendar cal = Calendar.getInstance();

		cal.set(Calendar.DATE, 1);
		return DateFormatUtils.format(cal, MONTH_FORMAT);

	}
	
	
	
	/**
	 * @Title:getWeekMondayDay
	 * @Description: 得到当前周的第一天.
	 * @return
	 * @return String
	 */
	public static String getWeekMonday() {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		return DateFormatUtils.format(cal, DATE_FORMAT);

	}

	/**
	 * @Title:getMonthLastDay
	 * @Description: 得到当前月最后一天
	 * @return
	 * @return String
	 */
	public static String getMonthLastDay() {
		Calendar cal = Calendar.getInstance();

		cal.set(Calendar.DATE, 1);// 设为当前月的1号
		cal.add(Calendar.MONTH, 1);// 加一个月，变为下月的1号
		cal.add(Calendar.DATE, -1);// 减去一天，变为当月最后一天
		return DateFormatUtils.format(cal, DATE_FORMAT);
	}

	/**
	 * @Title:getPreviousMonthFirst
	 * @Description: 得到上个月的第一天
	 * @return
	 * @return String
	 */
	public static String getPreviousMonthFirst() {
		Calendar cal = Calendar.getInstance();

		cal.set(Calendar.DATE, 1);// 设为当前月的1号
		cal.add(Calendar.MONTH, -1);
		return DateFormatUtils.format(cal, DATE_FORMAT);
	}

	/**
	 * @Title:getPreviousMonthEnd
	 * @Description: 得到上个月最后一天
	 * @return
	 * @return String
	 */
	public static String getPreviousMonthEnd() {
		Calendar cal = Calendar.getInstance();

		cal.set(Calendar.DATE, 1);// 设为当前月的1号
		cal.add(Calendar.MONTH, 0);//
		cal.add(Calendar.DATE, -1);// 减去一天，变为当月最后一天
		return DateFormatUtils.format(cal, DATE_FORMAT);
	}

	/**
	 * @Title:getNextMonthFirst
	 * @Description: 得到下个月的第一天
	 * @return
	 * @return String
	 */
	public static String getNextMonthFirst() {
		Calendar cal = Calendar.getInstance();
		Calendar f = (Calendar) cal.clone();
		f.clear();

		cal.set(Calendar.DATE, 1);// 设为当前月的1号
		cal.add(Calendar.MONTH, +1);// 加一个月，变为下月的1号
		return DateFormatUtils.format(cal, DATE_FORMAT);
	}

	/**
	 * @Title:getNextMonthEnd
	 * @Description: 得到下个月最后一天。
	 * @return
	 * @return String
	 */
	public static String getNextMonthEnd() {
		Calendar cal = Calendar.getInstance();

		cal.add(Calendar.MONTH, 1);// 加一个月
		cal.set(Calendar.DATE, 1);// 把日期设置为当月第一天
		cal.roll(Calendar.DATE, -1);// 日期回滚一天，也就是本月最后一天
		return DateFormatUtils.format(cal, DATE_FORMAT);
	}

	/**
	 * @Title:getCurrentMonthDays
	 * @Description: 得到当前月的天数
	 * @return
	 * @return int
	 */
	public static int getCurrentMonthDays() {
		Calendar cal = new GregorianCalendar();// Calendar.getInstance();
		int days = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
		return days;
	}

	/**
	 * @Title:getSpecifiedMonthDays
	 * @Description: 得到指定的月份的天数
	 * @param date
	 * @return
	 * @return int
	 */
	public static int getSpecifiedMonthDays(String date) {
		Calendar cal = Calendar.getInstance();
		try {
			cal.setTime(DateUtils.parseDate(date, MONTH_FORMAT));
			int days = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
			return days;
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		return 0;
	}

	/**
	 * @Title:getCurrentDate
	 * @Description: 得到当前日期
	 * @return
	 * @return String
	 */
	public static String getCurrentDate() {
		Calendar cal = Calendar.getInstance();
		String currentDate = DateFormatUtils.format(cal, DATE_FORMAT);
		return currentDate;
	}
	
	public static String getYesterdayDate() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -1);
		String yesterdayDate = DateFormatUtils.format(cal, DATE_FORMAT);
		return yesterdayDate;
	}


	/**
	 * @Title:getCurrentTime
	 * @Description: 得到当前的时间
	 * @return
	 * @return String
	 */
	public static String getCurrentTime() {
		Calendar cal = Calendar.getInstance();
		String currentDate = DateFormatUtils.format(cal, TIME_FORMAT);
		return currentDate;
	}

	/**
	 * @Title:getOffsetDate
	 * @Description: 得到与当前日期偏移量为X的日期。
	 * @param offset
	 * @return
	 * @return String
	 */
	public static String getOffsetDate(int offset) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_MONTH, offset);
		String currentDate = DateFormatUtils.format(cal, DATE_FORMAT);
		return currentDate;
	}

	/**
	 * @Title:getSpecifiedOffsetDate
	 * @Description: 得到与指定日期偏移量为X的日期。
	 * @param specifiedDate指定的日期
	 *            ,格式为YYYY-MM-DD
	 * @param offset
	 * @return 返回yyyy-MM-dd格式的字符串日期
	 * @return String
	 * @throws ParseException
	 */
	public static String getSpecifiedOffsetDate(String specifiedDate, int offset)
			throws ParseException {
		Date date = DateUtils.parseDate(specifiedDate, DATE_FORMAT);
		Calendar cal = DateUtils.toCalendar(date);
		cal.add(Calendar.DAY_OF_MONTH, offset);
		String returnDate = DateFormatUtils.format(cal, DATE_FORMAT);
		return returnDate;
	}

	/**
	 * @Title:getSpecifiedOffsetTime
	 * @Description: 得到与指定日期时间偏移量为X的时间。
	 * @param specifiedTime
	 *            指定的时间,格式为yyyy-MM-dd HH:mm:ss
	 * @param offset
	 *            偏移天数
	 * @return 返回yyyy-MM-dd HH:mm:ss格式的字符串时间
	 * @throws ParseException
	 * @return String
	 */
	public static String getSpecifiedOffsetTime(String specifiedTime, int offset)
			throws ParseException {
		Date date = DateUtils.parseDate(specifiedTime, TIME_FORMAT);
		Calendar cal = DateUtils.toCalendar(date);
		cal.add(Calendar.DAY_OF_MONTH, offset);
		String returnDate = DateFormatUtils.format(cal, TIME_FORMAT);
		return returnDate;
	}

	/**
	 * @Title:getOffsetDateTime
	 * @Description: 得到与指定日期时间偏移量为X的时间。
	 * @param specifiedDateTime
	 *            指定的时间,格式为yyyy-MM-dd HH:mm:ss/yyyy-MM-dd
	 * @param offset
	 *            偏移天数
	 * @return
	 * @throws ParseException
	 * @return String
	 */
	public static String getOffsetDateTime(String specifiedDateTime, int offset)
			throws ParseException {
		if (offset == 0) {
			return specifiedDateTime;
		}
		String regexStr = "\\d{4}-\\d{2}-\\d{2}";
		if (specifiedDateTime.matches(regexStr)) {
			return getSpecifiedOffsetDate(specifiedDateTime, offset);
		} else {
			return getSpecifiedOffsetTime(specifiedDateTime, offset);
		}
	}

	/**
	 * 判断是否为润年
	 * 
	 * @param year
	 * @return
	 */
	public static boolean isLeapYear(int year) {
		return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
	}

	/**
	 * @Title:getWeekDay
	 * @Description: 判断是星期几.
	 * @param c
	 * @return
	 * @return String
	 */
	public static String getWeekDay(Calendar c) {
		if (c == null) {
			return "星期一";
		}
		switch (c.get(Calendar.DAY_OF_WEEK)) {
		case Calendar.MONDAY:
			return "星期一";
		case Calendar.TUESDAY:
			return "星期二";
		case Calendar.WEDNESDAY:
			return "星期三";
		case Calendar.THURSDAY:
			return "星期四";
		case Calendar.FRIDAY:
			return "星期五";
		case Calendar.SATURDAY:
			return "星期六";
		default:
			return "星期日";
		}
	}

	public static int getWeekDayCode(Calendar c) {
		if (c == null) {
			return 0;
		}
		switch (c.get(Calendar.DAY_OF_WEEK)) {
		case Calendar.MONDAY:
			return MONDAY;
		case Calendar.TUESDAY:
			return TUESDAY;
		case Calendar.WEDNESDAY:
			return WEDNESDAY;
		case Calendar.THURSDAY:
			return THURSDAY;
		case Calendar.FRIDAY:
			return FRIDAY;
		case Calendar.SATURDAY:
			return SATURDAY;
		default:
			return SUNDAY;
		}
	}
	
	public static boolean isBeginningOfWeek(Calendar c) {
		if (c == null) {
			return false;
		}
		
		 if(MONDAY == getWeekDayCode(c)) {
			 return true;
		 }
		
		return false;
	}

	public static boolean isBeginningOfMonth(Calendar c) {
		if (c == null) {
			return false;
		}

		if (c.get(Calendar.DAY_OF_MONTH) == 1) {
			return true;
		}

		return false;
	}

	/**
	 * 
	 * @param begin
	 * @param end
	 * @return
	 */
	public static List<String> getWeekListBetweenDates(int begin, String end) {
		List<String> dateList = new ArrayList<String>();

		try {

			Calendar calendar = DateUtils.toCalendar(DateUtils.parseDate(end,
					DATE_FORMAT));

			// 最后时间属于星期几
			int dayOfWeek = getWeekDayCode(calendar);
			// 需要向周一的偏移量
			int offset = dayOfWeek - MONDAY;
			// 得到结束时间
			Date endDate = DateUtils.parseDate(getOffsetDateTime(end, -offset),
					DATE_FORMAT);
			// 得到开始时间
			Date startDate = DateUtils.parseDate(
					getOffsetDateTime(getOffsetDateTime(end, -offset),
							(begin * -SUNDAY)), DATE_FORMAT);

			if (startDate.compareTo(endDate) > 0) {
				return dateList;
			}
			do {
				dateList.add(DateFormatUtils.format(startDate, DATE_FORMAT));
				startDate = DateUtils.addWeeks(startDate, 1);

			} while (startDate.compareTo(endDate) <= 0);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		return dateList;
	}

	/**
	 * @Title:getDaysListBetweenDates
	 * @Description: 获得两个日期之间的连续日期.
	 * @param begin
	 *            开始日期 .
	 * @param end
	 *            结束日期 .
	 * @return
	 * @return List<String>
	 */
	public static List<String> getDaysListBetweenDates(String begin, String end) {
		List<String> dateList = new ArrayList<String>();
		Date d1;
		Date d2;
		try {
			d1 = DateUtils.parseDate(begin, DATE_FORMAT);
			d2 = DateUtils.parseDate(end, DATE_FORMAT);
			if (d1.compareTo(d2) > 0) {
				return dateList;
			}
			do {
				dateList.add(DateFormatUtils.format(d1, DATE_FORMAT));
				d1 = DateUtils.addDays(d1, 1);
			} while (d1.compareTo(d2) <= 0);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return dateList;
	}

	public static List<String> getDaysListBetweenDates(int begin, String end) {
		String beginDate = "";
		try {

			beginDate = getOffsetDateTime(end, -begin);

		} catch (ParseException e) {
			e.printStackTrace();
		}

		return getDaysListBetweenDates(beginDate, end);
	}

	public static List<String> getMonthsListBetweenDates(int begin, String end) {
		String beginDate = "";
		try {
			Date endDate = DateUtils.parseDate(end, DATE_FORMAT);

			Date startDate = DateUtils.addMonths(endDate, -begin);

			SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

			beginDate = sdf.format(startDate);

		} catch (ParseException e) {
			e.printStackTrace();
		}
		return getMonthsListBetweenDates(beginDate, end);
	}

	/**
	 * @Title:getMonthsListBetweenDates
	 * @Description: 获得连续的月份
	 * @param begin
	 * @param end
	 * @return
	 * @return List<String>
	 */
	public static List<String> getMonthsListBetweenDates(String begin,
			String end) {
		List<String> dateList = new ArrayList<String>();
		Date d1;
		Date d2;
		try {
			d1 = DateUtils.parseDate(begin, DATE_FORMAT);
			d2 = DateUtils.parseDate(end, DATE_FORMAT);
			if (d1.compareTo(d2) > 0) {
				return dateList;
			}
			do {
				dateList.add(DateFormatUtils.format(d1, MONTH_FORMAT));
				d1 = DateUtils.addMonths(d1, 1);
			} while (d1.compareTo(d2) <= 0);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return dateList;
	}

	/**
	 * @Title:long2Time
	 * @Description: 将long类型的时间值转换成标准格式的时间（yyyy-MM-dd HH:mm:ss）
	 * @param createTime
	 * @return
	 * @return String
	 */
	public static String long2Time(String createTime) {
		long fooTime = Long.parseLong(createTime) * 1000L;
		return DateFormatUtils.format(fooTime, TIME_FORMAT);
	}

	public static List<String> getQueryDates(String scale, int dateRange) {
		String endDate = GaDateUtils.getCurrentDate();
		List<String> datas = null;
		if (scale.equals(GaDateUtils.DAY)) {
			datas = GaDateUtils.getDaysListBetweenDates(dateRange, endDate);
		} else if (scale.equals(GaDateUtils.WEEK)) {
			datas = GaDateUtils.getWeekListBetweenDates(dateRange, endDate);
		} else if (scale.equals(GaDateUtils.MONTH)) {
			datas = GaDateUtils.getMonthsListBetweenDates(dateRange, endDate);
		}

		return datas;
	}

	// 获取查询时期

	public static void main(String[] args) throws ParseException {
		System.out.println(getMonthFirstDay());
		System.out.println(getMonthLastDay());
		System.out.println(getPreviousMonthFirst());
		System.out.println(getPreviousMonthEnd());
		System.out.println(getNextMonthFirst());
		System.out.println(getNextMonthEnd());
		System.out.println(getCurrentMonthDays());
		System.out.println(getSpecifiedMonthDays("1900-02"));
		System.out.println(getCurrentDate());
		System.out.println(getOffsetDate(-4));
		System.out.println(isLeapYear(1900));
		System.out.println(getWeekDay(Calendar.getInstance()));
		System.out.println(getDaysListBetweenDates("2012-1-12", "2012-1-21"));

		System.out.println(getSpecifiedOffsetTime("2012-09-09 12:12:12", 12));

		System.out.println(getOffsetDateTime("2015-09-04", -3));

		System.out.println(getOffsetDateTime("2012-09-09 12:12:12", 12));
		System.out.println(long2Time("1234567890"));

		System.out.println("---------------------------");

		//System.out.println(getDaysListBetweenDates(3, "2015-10-16"));

		System.out.println(getWeekListBetweenDates(1, "2015-10-16"));

		System.out.println(getMonthsListBetweenDates(1, "2012-01-21"));

		//System.out.println(isBeginningOfMonth(Calendar.getInstance()));
		
		//System.out.println(isBeginningOfWeek(Calendar.getInstance()));
		
	}
}
