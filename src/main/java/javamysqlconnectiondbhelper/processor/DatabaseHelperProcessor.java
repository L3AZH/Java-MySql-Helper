package javamysqlconnectiondbhelper.processor;

import com.squareup.javapoet.*;
import javamysqlconnectiondbhelper.Constant;
import javamysqlconnectiondbhelper.annotations.DatabaseHelper;
import javamysqlconnectiondbhelper.annotations.Repository;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SupportedAnnotationTypes("javamysqlconnectiondbhelper.annotations.*")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class DatabaseHelperProcessor extends AbstractProcessor {

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        Set<? extends Element> elementsHelper = roundEnv.getElementsAnnotatedWith(DatabaseHelper.class);
        Set<? extends Element> elementsRepo = roundEnv.getElementsAnnotatedWith(Repository.class);
        Set<? extends Element> rootE=roundEnv.getRootElements();
        for (Element element : elementsHelper) {
            DatabaseHelper dbHelperAnnotation = element.getAnnotation(DatabaseHelper.class);
            //check if it is interface or class ?
            if (element.getKind().isInterface()) {
                String fileGenName = element.getSimpleName().toString().concat("Impl.java");
                String packageName = ((PackageElement) element.getEnclosingElement()).getQualifiedName().toString();
                String connectionString = String.format(
                        Constant.TEMPLATE_CONNECTION_STRING,
                        dbHelperAnnotation.username(),
                        dbHelperAnnotation.password(),
                        dbHelperAnnotation.host(),
                        dbHelperAnnotation.port(),
                        dbHelperAnnotation.database());
                if (tryToConnectionBeforeGeneration(connectionString)) {
                    /**
                     * Generate Class Helper
                     */
                    generateHelperFile(fileGenName, packageName, connectionString);
                } else {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "Can not connect to database to generation file");
                }

                try {
                    configRepoFileWithHelper(element, elementsRepo);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }

            } else {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Annotation not for Class");
                return false;
            }
        }
        return false;
    }

    private void configRepoFileWithHelper(Element elementHelper, Set<? extends Element> elementsRepo) throws ClassNotFoundException {
        /**
         * Generate Class Repository
         */
        for (Element elementRepo : elementsRepo) {
            System.out.println();
            AnnotationMirror annotationMirror = getAnnotationMirror((TypeElement) elementRepo, Repository.class);
            if (annotationMirror == null) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Can not found Repository.class ");
                continue;
            }
            AnnotationValue annotationValue = getAnnotationValue(annotationMirror, "helper");
            /**
             * @Author Lam Ha Tuan Anh
             * @Note
             * there are 2 way to get the class name annotate:
             * String helperInterfaceName =
             *                     ((PackageElement) elementHelper.getEnclosingElement()).getQualifiedName().toString()
             *                     .concat(".")
             *                     .concat(elementHelper.getSimpleName().toString());
             * String helperInterfaceName2 = elementHelper.toString();

             */
            if (!elementHelper.toString().equals(annotationValue.getValue().toString())) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Class does not match with " + DatabaseHelper.class.getName());
                continue;
            }
        }
    }


    private boolean tryToConnectionBeforeGeneration(String connectionString) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connection = DriverManager.getConnection(connectionString);
            PreparedStatement ps = connection.prepareStatement("SELECT DATABASE();");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.OTHER,
                        "Connected to database '" + rs.getString(1) + "' success");
            }
            rs.close();
            ps.close();
            return true;
        } catch (SQLException | ClassNotFoundException exception) {
            exception.printStackTrace();
            return false;
        }
    }

    private void generateHelperFile(String fileName, String packageName, String dbConString) {
        FieldSpec connectionField = FieldSpec.builder(Connection.class, "connection")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .build();
        MethodSpec getConnectionMethod = MethodSpec.methodBuilder("getConnection")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .beginControlFlow("try")
                .beginControlFlow("if ($N== null)", connectionField.name)
                .addStatement("$N = $T.getConnection($S)", connectionField.name, DriverManager.class, dbConString)
                .endControlFlow()
                .nextControlFlow("catch($T e)", SQLException.class)
                .addStatement("e.printStackTrace()")
                .endControlFlow()
                .addStatement("return $N", connectionField.name)
                .returns(Connection.class)
                .build();
        TypeSpec helperClass = TypeSpec.classBuilder(fileName.split("\\.")[0])
                .addModifiers(Modifier.PUBLIC)
                .addField(connectionField)
                .addMethod(getConnectionMethod)
                .build();
        JavaFile javaFile = JavaFile.builder(packageName, helperClass)
                .build();
        try {
            FileObject file = processingEnv.getFiler().createResource(
                    StandardLocation.SOURCE_OUTPUT, "db_helper", fileName);
            javaFile.writeTo(new File(file.toUri().getPath()));
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Can not generation file");
        }
    }


    private void generateRepoFile(String fileName, String packageName) {

    }

    private static AnnotationMirror getAnnotationMirror(TypeElement typeElement, Class<?> clazz) {
        String clazzName = clazz.getName();
        for (AnnotationMirror m : typeElement.getAnnotationMirrors()) {
            if (m.getAnnotationType().toString().equals(clazzName)) {
                return m;
            }
        }
        return null;
    }

    private static AnnotationValue getAnnotationValue(AnnotationMirror annotationMirror, String key) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationMirror.getElementValues().entrySet()) {
            if (entry.getKey().getSimpleName().toString().equals(key)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
