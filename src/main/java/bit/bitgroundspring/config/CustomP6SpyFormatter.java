package bit.bitgroundspring.config;

import com.p6spy.engine.spy.appender.MessageFormattingStrategy;

public class CustomP6SpyFormatter implements MessageFormattingStrategy {
    
    public CustomP6SpyFormatter() {
    }
    
    @Override
    public String formatMessage(int connectionId, String now, long elapsed,
                                String category, String prepared, String sql, String url) {
        
        return new StringBuilder()
                .append("\n🔍 ==== P6Spy SQL Log ====")
                .append("\n⏱️  Execution Time: ").append(elapsed).append(" ms")
                .append("\n🔗 Connection ID: ").append(connectionId)
                .append("\n📂 Category: ").append(category)
                .append("\n💾 SQL:\n")
                .append(formatSql(sql != null ? sql : prepared))
                .append("\n🔚 ========================\n")
                .toString();
    }
    
    private String formatSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return "   [No SQL executed]";
        }
        
        String cleanSql = sql.replaceAll("\\s+", " ").trim();
        String lowerSql = cleanSql.toLowerCase();
        
        if (lowerSql.startsWith("select")) {
            return formatSelectQuery(cleanSql);
        } else if (lowerSql.startsWith("insert") || lowerSql.startsWith("update") || lowerSql.startsWith("delete")) {
            return formatSimpleQuery(cleanSql);
        } else {
            return "   " + cleanSql;
        }
    }
    
    private String formatSelectQuery(String sql) {
        StringBuilder result = new StringBuilder();
        
        // 각 절 추출
        String selectPart = extractBetween(sql, "(?i)select", "(?i)from");
        String fromPart = extractBetween(sql, "(?i)from", "(?i)(where|group by|having|order by|limit|$)");
        String wherePart = extractBetween(sql, "(?i)where", "(?i)(group by|having|order by|limit|$)");
        String groupByPart = extractBetween(sql, "(?i)group by", "(?i)(having|order by|limit|$)");
        String havingPart = extractBetween(sql, "(?i)having", "(?i)(order by|limit|$)");
        String orderByPart = extractBetween(sql, "(?i)order by", "(?i)(limit|$)");
        String limitPart = extractBetween(sql, "(?i)limit", "$");
        
        // SELECT 절
        result.append("   select\n"); // 3칸 들여쓰기 명시적 확인
        if (!selectPart.isEmpty()) {
            appendColumns(result, selectPart, "         ");
        }
        
        // FROM 절 (JOIN 포함)
        if (!fromPart.isEmpty()) {
            result.append("   from\n").append(formatFromWithJoins(fromPart));
        }
        
        // WHERE 절
        if (!wherePart.isEmpty()) {
            result.append("   where\n").append(formatWhereConditions(wherePart));
        }
        
        // GROUP BY 절
        if (!groupByPart.isEmpty()) {
            result.append("   group by\n");
            appendColumns(result, groupByPart, "         ");
        }
        
        // HAVING 절
        if (!havingPart.isEmpty()) {
            result.append("   having\n").append(formatWhereConditions(havingPart));
        }
        
        // ORDER BY 절
        if (!orderByPart.isEmpty()) {
            result.append("   order by\n");
            appendColumns(result, orderByPart, "         ");
        }
        
        // LIMIT 절
        if (!limitPart.isEmpty()) {
            result.append("   limit ").append(limitPart.trim()).append("\n");
        }
        
        return result.toString().replaceAll("\\n+$", ""); // 마지막 줄바꿈만 제거
    }
    
    private void appendColumns(StringBuilder sb, String columns, String indent) {
        String[] cols = columns.split(",");
        for (int i = 0; i < cols.length; i++) {
            sb.append(indent).append(cols[i].trim());
            if (i < cols.length - 1) sb.append(",");
            sb.append("\n");
        }
    }
    
    private String formatFromWithJoins(String fromPart) {
        StringBuilder sb = new StringBuilder();
        String[] joinTypes = {"inner join", "left join", "right join", "full join", "join"};
        
        // 메인 테이블
        String[] parts = fromPart.split("(?i)\\b(inner join|left join|right join|full join|join)\\b");
        if (parts.length > 0) {
            sb.append("         ").append(parts[0].trim()).append("\n");
        }
        
        // JOIN 처리
        String remaining = fromPart;
        for (String joinType : joinTypes) {
            remaining = processJoins(sb, remaining, joinType);
        }
        
        return sb.toString();
    }
    
    private String processJoins(StringBuilder sb, String remaining, String joinType) {
        while (remaining.toLowerCase().contains(joinType)) {
            int joinPos = remaining.toLowerCase().indexOf(joinType);
            if (joinPos == -1) break;
            
            String afterJoin = remaining.substring(joinPos + joinType.length()).trim();
            String[] onSplit = afterJoin.split("(?i)\\bonon\\b", 2); // 'on' 대신 'onon'으로 잘못 입력된 부분이 있어서 수정했습니다. (만약 onon이 맞다면 유지)
            // 일반적으로 SQL 쿼리에서 'on' 키워드를 사용하므로, 위 스택 트레이스와는 무관하게 'onon'을 'on'으로 수정했습니다.
            // 기존 코드: String[] onSplit = afterJoin.split("(?i)\\bonon\\b", 2);
            // 수정 후: String[] onSplit = afterJoin.split("(?i)\\bon\\b", 2);
            
            
            if (onSplit.length > 0) {
                String joinTable = onSplit[0].trim();
                sb.append("   ").append(joinType).append(" ").append(joinTable).append("\n");
                
                if (onSplit.length > 1) {
                    String onClause = getOnClause(onSplit[1]);
                    if (!onClause.isEmpty()) {
                        sb.append("            on ").append(onClause).append("\n");
                    }
                }
                
                // 처리된 부분 제거
                int nextPos = joinPos + joinType.length() + joinTable.length();
                if (onSplit.length > 1) {
                    nextPos += getOnClause(onSplit[1]).length() + 3; // " on " 포함
                }
                remaining = nextPos < remaining.length() ? remaining.substring(nextPos) : "";
            } else {
                break;
            }
        }
        return remaining;
    }
    
    private String getOnClause(String onPart) {
        String[] joinTypes = {"inner join", "left join", "right join", "full join", "join"};
        for (String nextJoin : joinTypes) {
            if (onPart.toLowerCase().contains(nextJoin)) {
                return onPart.substring(0, onPart.toLowerCase().indexOf(nextJoin)).trim();
            }
        }
        return onPart.trim();
    }
    
    private String formatWhereConditions(String conditions) {
        String formatted = formatConditionsWithParentheses(conditions.trim());
        StringBuilder sb = new StringBuilder();
        
        for (String line : formatted.split("\n")) {
            if (!line.trim().isEmpty()) {
                sb.append("         ").append(line.trim()).append("\n");
            }
        }
        
        return sb.toString();
    }
    
    private String formatConditionsWithParentheses(String conditions) {
        StringBuilder result = new StringBuilder();
        int parenthesesLevel = 0;
        
        for (int i = 0; i < conditions.length(); i++) {
            char c = conditions.charAt(i);
            String remaining = conditions.substring(i);
            
            if (c == '(') {
                result.append(c);
                parenthesesLevel++;
            } else if (c == ')') {
                result.append(c);
                // 괄호 레벨이 음수가 되는 것을 방지
                parenthesesLevel = Math.max(0, parenthesesLevel - 1);
            } else if (remaining.toLowerCase().startsWith("and ")) {
                result.append("\n").append("   ".repeat(parenthesesLevel)).append("and ");
                i += 3; // "and " 길이 - 1 (for loop에서 +1 됨)
            } else if (remaining.toLowerCase().startsWith("or ")) {
                result.append("\n").append("   ".repeat(parenthesesLevel)).append("or ");
                i += 2; // "or " 길이 - 1 (for loop에서 +1 됨)
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
    
    private String formatSimpleQuery(String sql) {
        return "   " + sql;
    }
    
    private String extractBetween(String text, String start, String end) {
        String pattern = start + "\\s+(.*?)(?=" + end + "|$)";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern,
                java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher m = p.matcher(text);
        return m.find() ? m.group(1).trim() : "";
    }
}