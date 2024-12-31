# jacoco-maven-plugin

```xml
<plugin>
    <groupId>com.lrenyi</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>jacoco-report</id>
            <phase>verify</phase>
            <goals>
                <goal>report-aggregate</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```
以上配置会将当前module的父模块下的所有module的单元测试报告聚合