package krys.itemimport.image;

import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Jawnie oznacza test uruchamiający prawdziwe rozpoznawanie Tess4J/Tesseract. */
@Tag("ocr-integration")
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface OcrIntegrationTest {
}
