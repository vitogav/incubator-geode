/*=========================================================================
 * Copyright Copyright (c) 2000-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * more patents listed at http://www.pivotal.io/patents.
 * $Id: Functions.java,v 1.1 2005/01/27 06:26:33 vaibhav Exp $
 *=========================================================================
 */

package com.gemstone.gemfire.cache.query.internal;

import java.util.*;

import com.gemstone.gemfire.cache.query.*;
import com.gemstone.gemfire.internal.i18n.LocalizedStrings;
import com.gemstone.gemfire.pdx.internal.PdxString;

import java.text.*;

/**
 * Class Description
 *
 * @version     $Revision: 1.1 $
 * @author      ericz
 */

public class Functions
{
    public static final int ELEMENT = 1;

    public static Object nvl(CompiledValue arg1, CompiledValue arg2, ExecutionContext context)
      throws FunctionDomainException, TypeMismatchException, NameResolutionException,
      QueryInvocationTargetException {
      Object value = arg1.evaluate(context);
      if(value == null) {
        return arg2.evaluate(context);
      }
      return value;
    }

    public static Date to_date (CompiledValue cv1, CompiledValue cv2, ExecutionContext context)
    throws FunctionDomainException, TypeMismatchException, NameResolutionException,
    QueryInvocationTargetException {
      Object value1 = cv1.evaluate(context);
      Object value2 = cv2.evaluate(context);
      if(!(value1 instanceof String) || !(value2 instanceof String)) {
		throw new QueryInvalidException(LocalizedStrings.Functions_PARAMETERS_TO_THE_TO_DATE_FUNCTION_SHOULD_BE_STRICTLY_SIMPLE_STRINGS.toLocalizedString());
      }
      String dateStr = (String) value1;
      String format = (String) value2;
      Date dt=null;		
      try {
        // Removed the following line so that to data format conforms to
        // SimpleDateFormat exactly (bug 39144)
        //format = ((format.replaceAll("Y", "y")).replaceAll("m", "M")).replaceAll("D", "d");

        /*if((format.indexOf("MM") == -1 && format.indexOf("M") == -1) || (format.
  indexOf("dd") == -1 && format.indexOf("d") == -1) || (format.indexOf("yyyy") ==
  -1 && format.indexOf("yy") == -1))
        {
          throw new QueryInvalidException("Malformed date format string");
        }
        if(format.indexOf("MMM") != -1 || format.indexOf("ddd") != -1 || format.in
  dexOf("yyyyy") != -1)
        {
          throw new QueryInvalidException("Malformed date format string");
        } */

        SimpleDateFormat sdf1 = new SimpleDateFormat(format);
        dt = sdf1.parse(dateStr);

      }
      catch (Exception ex){
        throw new QueryInvalidException(LocalizedStrings.Functions_MALFORMED_DATE_FORMAT_STRING_AS_THE_FORMAT_IS_0.toLocalizedString(format), ex);
      }
      return dt;
    }
    //end of to_date

    public static Object element(Object arg, ExecutionContext context)
        throws FunctionDomainException, TypeMismatchException
    {
        if (arg == null || arg == QueryService.UNDEFINED)
            return QueryService.UNDEFINED;

        if (arg instanceof Collection)
        {
            Collection c = (Collection)arg;
            // for remote distinct queries, the result of sub query could contain a
            // mix of String and PdxString which could be duplicates, so convert all 
            // PdxStrings to String
            if (context.isDistinct() && ((DefaultQuery)context.getQuery()).isRemoteQuery()) {
              Set tempResults = new HashSet();
              for (Object o : c) {
                if(o instanceof PdxString) {
                  o = ((PdxString)o).toString();
                }
                tempResults.add(o);
              }
              c.clear();
              c.addAll(tempResults);
              tempResults = null;
            }
            checkSingleton(c.size());
            return c.iterator().next();
        }

        // not a Collection, must be an array
        Class clazz = arg.getClass();
        if (!clazz.isArray())
            throw new TypeMismatchException(LocalizedStrings.Functions_THE_ELEMENT_FUNCTION_CANNOT_BE_APPLIED_TO_AN_OBJECT_OF_TYPE_0.toLocalizedString(clazz.getName()));

        // handle arrays
        if (arg instanceof Object[])
        {
            Object[] a = (Object[])arg;
            if (((DefaultQuery) context.getQuery()).isRemoteQuery() && context.isDistinct()) {
              for ( int i = 0; i < a.length; i++) {
                if(a[i] instanceof PdxString) {
                  a[i] = ((PdxString)a[i]).toString();
                }
              }
            }
            checkSingleton(a.length);
            return a[0];
        }

        if (arg instanceof int[])
        {
            int[] a = (int[])arg;
            checkSingleton(a.length);
            return Integer.valueOf(a[0]);
        }

        if (arg instanceof long[])
        {
            long[] a = (long[])arg;
            checkSingleton(a.length);
            return Long.valueOf(a[0]);
        }


        if (arg instanceof boolean[])
        {
            boolean[] a = (boolean[])arg;
            checkSingleton(a.length);
            return Boolean.valueOf(a[0]);
        }

        if (arg instanceof byte[])
        {
            byte[] a = (byte[])arg;
            checkSingleton(a.length);
            return Byte.valueOf(a[0]);
        }

        if (arg instanceof char[])
        {
            char[] a = (char[])arg;
            checkSingleton(a.length);
            return new Character(a[0]);
        }

        if (arg instanceof double[])
        {
            double[] a = (double[])arg;
            checkSingleton(a.length);
            return Double.valueOf(a[0]);
        }

        if (arg instanceof float[])
        {
            float[] a = (float[])arg;
            checkSingleton(a.length);
            return new Float(a[0]);
        }


        if (arg instanceof short[])
        {
            short[] a = (short[])arg;
            checkSingleton(a.length);
            return new Short(a[0]);
        }

        // did I miss something?
        throw new TypeMismatchException(LocalizedStrings.Functions_THE_ELEMENT_FUNCTION_CANNOT_BE_APPLIED_TO_AN_OBJECT_OF_TYPE_0.toLocalizedString(clazz.getName()));
    }

    private static void checkSingleton(int size)
        throws FunctionDomainException
    {
        if (size != 1)
            throw new FunctionDomainException(LocalizedStrings.Functions_ELEMENT_APPLIED_TO_PARAMETER_OF_SIZE_0.toLocalizedString(Integer.valueOf(size)));
    }


}
