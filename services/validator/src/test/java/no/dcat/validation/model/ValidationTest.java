package no.dcat.validation.model;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

/**
 * Created by dask on 05.05.2017.
 */
public class ValidationTest {

    @Test
    public void instanciateValidation() throws Throwable {
        Validation validation = new Validation();

        validation.setErrors(2);
        validation.setWarnings(3);
        validation.setOks(1);

        assertThat(validation.getErrors(), is(2));
        assertThat(validation.getWarnings(), is(3));
        assertThat(validation.getOks(), is(1));

        assertThat(validation.getPropertyReport(), is(empty()));

    }
}