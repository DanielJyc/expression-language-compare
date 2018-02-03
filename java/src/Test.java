import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ql.util.express.DefaultContext;
import com.ql.util.express.ExpressRunner;
import de.odysseus.el.ExpressionFactoryImpl;
import de.odysseus.el.util.SimpleContext;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;

import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Created by DanielJyc on 2018-02-01 20:26.
 */
public class Test {
    public static void main(String[] args) throws Exception {
        // 最常见的场景：多个条件进行and操作
        String expression = " a<100 && b>=100 && c<=123";
        System.out.println("\n执行100万次: " + expression);
        executeExpress(expression);

        // 包含特殊的操作：contains
        expression = "a<100 && b>=100 && c<=123 && stringList.contains(str)";
        System.out.println("\n执行100万次: " + expression);
        executeExpress(expression);

        // 一个稍微复杂一点的语法树
        expression = "a>1 && ((b>1 || c<1) || (a>1 && b<1 && c>1))";
        System.out.println("\n执行100万次: " + expression);
        executeExpress(expression);
    }

    private static void executeExpress(String expression) throws Exception {
        // 测试比较表达式：a>100 && b<100 && c>=123
        Integer aValue = 12;
        Integer bValue = 122;
        Integer cValue = 45;
        List<String> stringList = Lists.newArrayList("hello", "world");
        String str = "hello";

        long costTimeForQLExpress = 0;
        long costTimeForMvel1 = 0;
        long costTimeForMvel2 = 0;
        long costTimeForMvel3 = 0;
        long costTimeJuel = 0;

        for (int i = 0; i < 10; i++) {
            // QLExpress
            ExpressRunner runner = new ExpressRunner();
            {
                long beginTime = System.currentTimeMillis();

                for (int j = 0; j < 100000; j++) {
                    DefaultContext<String, Object> context = new DefaultContext<String, Object>();
                    context.put("a", aValue + j % 10);
                    context.put("b", bValue);
                    context.put("c", cValue);
                    context.put("stringList", stringList);
                    context.put("str", str);
                    runner.execute(expression, context, null, true, false);
                }
                long endTime = System.currentTimeMillis();
                costTimeForQLExpress += (endTime - beginTime);
            }

            // mvel 不编译
            {
                long beginTime = System.currentTimeMillis();

                for (int j = 0; j < 100000; j++) {
                    MVEL.eval(expression, getParamMap(aValue, bValue, cValue, stringList, str, j));
                }

                long endTime = System.currentTimeMillis();
                costTimeForMvel1 += (endTime - beginTime);
            }

            // mvel 先编译
            {
                long beginTime = System.currentTimeMillis();
                Serializable s = MVEL.compileExpression(expression);

                for (int j = 0; j < 100000; j++) {
                    MVEL.executeExpression(s, getParamMap(aValue, bValue, cValue, stringList, str, j));

                }

                long endTime = System.currentTimeMillis();
                costTimeForMvel2 += (endTime - beginTime);
            }

            // mvel 先编译，且输入值类型指定
            {
                long beginTime = System.currentTimeMillis();
                ParserContext context = ParserContext.create();
                context.addInput("a", aValue.getClass());
                context.addInput("b", bValue.getClass());
                context.addInput("c", cValue.getClass());
                Serializable s = MVEL.compileExpression(expression);

                for (int j = 0; j < 100000; j++) {
                    MVEL.executeExpression(s, getParamMap(aValue, bValue, cValue, stringList, str, j));
                }

                long endTime = System.currentTimeMillis();
                costTimeForMvel3 += (endTime - beginTime);
            }

            // juel
            {

                long beginTime = System.currentTimeMillis();
                // 1.Factory && Context
                ExpressionFactory factory = new ExpressionFactoryImpl();

                for (int j = 0; j < 100000; j++) {
                    // 2. Variables设置
                    SimpleContext context = new SimpleContext();
                    context.setVariable("a", factory.createValueExpression(aValue + j % 10, aValue.getClass()));
                    context.setVariable("b", factory.createValueExpression(bValue, bValue.getClass()));
                    context.setVariable("c", factory.createValueExpression(cValue, cValue.getClass()));
                    context.setVariable("stringList", factory.createValueExpression(stringList, List.class));
                    context.setVariable("str", factory.createValueExpression(str, String.class));

                    // 3.Parse && Evaluate
                    ValueExpression e = factory.createValueExpression(context, "${" + expression + "}", Boolean.class);
                    e.getValue(context);
                }
                long endTime = System.currentTimeMillis();
                costTimeJuel += (endTime - beginTime);
            }
        }

        // 输出结果 ms
        System.out.println("QLExpress 使用缓存:" + costTimeForQLExpress);
        System.out.println("mvel 不编译:" + costTimeForMvel1);
        System.out.println("mvel 先编译:" + costTimeForMvel2);
        System.out.println("mvel 先编译，且输入值类型指定:" + costTimeForMvel3);
        System.out.println("juel 输入值类型指定:" + costTimeJuel);
    }

    private static Map<String, Object> getParamMap(Integer aValue, Integer bValue, Integer cValue, List<String> stringList, String str, int j) {
        Map<String, Object> paramMap = Maps.newHashMap();
        paramMap.put("a", aValue + j % 10);
        paramMap.put("b", bValue);
        paramMap.put("c", cValue);
        paramMap.put("stringList", stringList);
        paramMap.put("str", str);
        return paramMap;
    }

}
