package com.blackducksoftware.integration.email.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;

import com.blackducksoftware.integration.email.service.properties.ServicePropertiesBuilder;
import com.blackducksoftware.integration.email.service.properties.ServicePropertyDescriptor;

public class ServicePropertiesBuilderTest {

	private File generatedFile;

	@After
	public void deleteGeneratedFile() {
		if (generatedFile.exists()) {
			generatedFile.delete();
		}
	}

	private Properties createTestPropertiesFile(final File file) throws FileNotFoundException, IOException {
		final Properties props = new Properties();
		try (final FileOutputStream output = new FileOutputStream(file)) {
			for (final ServicePropertyDescriptor descriptor : ServicePropertyDescriptor.values()) {
				// save the key as the value to be different than default value
				// for testing.
				props.put(descriptor.getKey(), descriptor.getKey());
			}
			props.store(output, "Generated for unit test");
		}

		return props;
	}

	@Test
	public void testGeneratePropFileDefault() throws Exception {
		ServicePropertiesBuilder propBuilder = new ServicePropertiesBuilder();
		propBuilder = Mockito.spy(propBuilder);
		final Properties props = propBuilder.build();

		for (final ServicePropertyDescriptor descriptor : ServicePropertyDescriptor.values()) {
			assertEquals(descriptor.getDefaultValue(), props.getProperty(descriptor.getKey()));
		}
		generatedFile = new File(ServicePropertiesBuilder.DEFAULT_PROP_FILE_NAME);
	}

	@Test
	public void testGeneratePropFileWithDirectoryPath() throws Exception {
		final String directory = "build/resources/test";
		ServicePropertiesBuilder propBuilder = new ServicePropertiesBuilder();
		propBuilder = Mockito.spy(propBuilder);
		Mockito.doReturn(directory).when(propBuilder).getFilePath();
		final Properties props = propBuilder.build();
		generatedFile = new File(propBuilder.getFilePath(), ServicePropertiesBuilder.DEFAULT_PROP_FILE_NAME);

		assertTrue(generatedFile.exists());

		for (final ServicePropertyDescriptor descriptor : ServicePropertyDescriptor.values()) {
			assertEquals(descriptor.getDefaultValue(), props.getProperty(descriptor.getKey()));
		}
	}

	@Test
	public void testGeneratePropFilePath() throws Exception {
		final String path = "build/resources/test/email.props";
		ServicePropertiesBuilder propBuilder = new ServicePropertiesBuilder();
		propBuilder = Mockito.spy(propBuilder);
		Mockito.doReturn(path).when(propBuilder).getFilePath();
		final Properties props = propBuilder.build();
		generatedFile = new File(propBuilder.getFilePath());

		assertTrue(generatedFile.exists());

		for (final ServicePropertyDescriptor descriptor : ServicePropertyDescriptor.values()) {
			assertEquals(descriptor.getDefaultValue(), props.getProperty(descriptor.getKey()));
		}
	}

	@Test
	public void testReadPropertiesFile() throws Exception {
		final String path = "build/resources/test/readTest.props";
		ServicePropertiesBuilder propBuilder = new ServicePropertiesBuilder();
		propBuilder = Mockito.spy(propBuilder);
		Mockito.doReturn(path).when(propBuilder).getFilePath();
		generatedFile = new File(propBuilder.getFilePath());
		final Properties existingProps = createTestPropertiesFile(generatedFile);

		final Properties props = propBuilder.build();
		for (final ServicePropertyDescriptor descriptor : ServicePropertyDescriptor.values()) {
			final String existing = existingProps.getProperty(descriptor.getKey());
			final String readProperty = props.getProperty(descriptor.getKey());
			assertEquals(existing, readProperty);
		}
	}

	@Test
	public void testReadDefaultPropertiesFile() throws Exception {
		final ServicePropertiesBuilder propBuilder = new ServicePropertiesBuilder();
		generatedFile = new File(ServicePropertiesBuilder.DEFAULT_PROP_FILE_NAME);
		final Properties existingProps = createTestPropertiesFile(generatedFile);

		final Properties props = propBuilder.build();
		for (final ServicePropertyDescriptor descriptor : ServicePropertyDescriptor.values()) {
			final String existing = existingProps.getProperty(descriptor.getKey());
			final String readProperty = props.getProperty(descriptor.getKey());
			assertEquals(existing, readProperty);
		}
	}
}
