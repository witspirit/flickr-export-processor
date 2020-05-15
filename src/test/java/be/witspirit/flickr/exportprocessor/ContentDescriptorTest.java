package be.witspirit.flickr.exportprocessor;

import org.junit.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class ContentDescriptorTest {

    @Test
    public void pattern2_sample1() {
        ContentDescriptor contentDescriptor = new ContentDescriptor(Path.of("20180830_kindjesphotoshoot---001_43536291795_o.jpg"));
        assertThat(contentDescriptor.getId()).isEqualTo("43536291795");
        assertThat(contentDescriptor.getName()).isEqualTo("20180830_kindjesphotoshoot---001");
        assertThat(contentDescriptor.getExtension()).isEqualTo("jpg");
    }


    @Test
    public void pattern2_sample2() {
        ContentDescriptor contentDescriptor = new ContentDescriptor(Path.of("050602-136_3977224776_o.jpg"));
        assertThat(contentDescriptor.getId()).isEqualTo("3977224776");
        assertThat(contentDescriptor.getName()).isEqualTo("050602-136");
        assertThat(contentDescriptor.getExtension()).isEqualTo("jpg");
    }

    @Test
    public void pattern1_sample1() {
        ContentDescriptor contentDescriptor = new ContentDescriptor(Path.of("19259555944_1777a52f53_o.jpg"));
        assertThat(contentDescriptor.getId()).isEqualTo("19259555944");
        assertThat(contentDescriptor.getName()).isEqualTo("1777a52f53");
        assertThat(contentDescriptor.getExtension()).isEqualTo("jpg");
    }



}
