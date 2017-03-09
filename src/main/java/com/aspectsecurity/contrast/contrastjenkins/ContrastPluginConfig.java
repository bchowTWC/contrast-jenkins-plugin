package com.aspectsecurity.contrast.contrastjenkins;

import com.contrastsecurity.exceptions.UnauthorizedException;
import com.contrastsecurity.models.Organizations;
import com.contrastsecurity.sdk.ContrastSDK;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.util.CopyOnWriteList;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Contrast Plugin Configuration
 * <p>
 * Adds the necessary configuration options to a job's properties. Used in VulnerabilityTrendRecorder
 */
public class ContrastPluginConfig extends JobProperty<AbstractProject<?, ?>> {
    private String teamServerProfileName;

    @DataBoundConstructor
    public ContrastPluginConfig() {

    }

    @Override
    public ContrastPluginConfigDescriptor getDescriptor() {
        Jenkins instance = Jenkins.getInstance();

        if (instance != null)
            return (ContrastPluginConfigDescriptor) instance.getDescriptor(getClass());
        else
            return null;
    }

    public TeamServerProfile getProfile() {
        return getProfile(teamServerProfileName);
    }

    public static TeamServerProfile getProfile(String profileName) {
        final TeamServerProfile[] profiles = new ContrastPluginConfigDescriptor().getTeamServerProfiles();

        if (profileName == null && profiles.length > 0)
            return profiles[0];

        for (TeamServerProfile profile : profiles) {
            if (profile.getName().equals(profileName))
                return profile;
        }
        return null;
    }

    @Extension
    public static class ContrastPluginConfigDescriptor extends JobPropertyDescriptor {

        private CopyOnWriteList<TeamServerProfile> teamServerProfiles = new CopyOnWriteList<>();

        public ContrastPluginConfigDescriptor() {
            super(ContrastPluginConfig.class);
            load();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            final JSONArray array = json.optJSONArray("profile");

            if (array != null) {
                teamServerProfiles.replaceBy(req.bindJSONToList(TeamServerProfile.class, array));
            } else {
                if (json.keySet().isEmpty()) {
                    teamServerProfiles = new CopyOnWriteList<>();
                } else {
                    teamServerProfiles.replaceBy(req.bindJSON(TeamServerProfile.class, json.getJSONObject("profile")));
                }
            }

            save();

            return true;
        }

        /** Validates the configured TeamServer profile by attempting to get the default profile for the username.
         *
         * @param username      String username for the TeamServer user
         * @param apiKey        String apiKey for the TeamServer user
         * @param serviceKey    String serviceKey for the TeamServer user
         * @param teamServerUrl String TeamServer Url for
         * @return FormValidation
         * @throws IOException
         * @throws ServletException
         */
        public FormValidation doTestTeamServerConnection(@QueryParameter("ts.username") final String username,
                                                         @QueryParameter("ts.apiKey") final String apiKey,
                                                         @QueryParameter("ts.serviceKey") final String serviceKey,
                                                         @QueryParameter("ts.teamServerUrl") final String teamServerUrl) throws IOException, ServletException {
            if (StringUtils.isEmpty(username)) {
                return FormValidation.error("TeamServer Connection error: Username cannot be empty.");
            }

            if (StringUtils.isEmpty(apiKey)) {
                return FormValidation.error("TeamServer Connection error: Api Key cannot be empty.");
            }

            if (StringUtils.isEmpty(serviceKey)) {
                return FormValidation.error("TeamServer Connection error: Service Key cannot be empty");
            }

            if (StringUtils.isEmpty(teamServerUrl)) {
                return FormValidation.error("TeamServer Connection error: TeamServer URL cannot be empty.");
            }

            if (!teamServerUrl.endsWith("/Contrast/api")) {
                return FormValidation.error("TeamServer Connection error: TeamServer URL does not end with /Contrast/api.");
            }

            try {
                ContrastSDK contrastSDK = new ContrastSDK(username, serviceKey, apiKey, teamServerUrl);

                Organizations organizations = contrastSDK.getProfileDefaultOrganizations();

                if (organizations == null || organizations.getOrganization() == null) {
                    return FormValidation.error("TeamServer Connection error: No organization found, Check your credentials and URL.");
                }

                return FormValidation.ok("Successfully verified the connection to TeamServer!");
            } catch (UnauthorizedException e) {
                return FormValidation.error("TeamServer Connection error: Unable to connect to TeamServer.");
            }
        }

        public TeamServerProfile[] getTeamServerProfiles() {
            final TeamServerProfile[] profileArray = new TeamServerProfile[teamServerProfiles.size()];
            return teamServerProfiles.toArray(profileArray);
        }

        @SuppressWarnings("unused")
        public ListBoxModel doFillTeamServerProfileNameItems() {
            final ListBoxModel model = new ListBoxModel();

            for (TeamServerProfile profile : teamServerProfiles) {
                model.add(profile.getName(), profile.getName());
            }

            return model;
        }

        /**
         * Validation of the 'username' form Field.
         *
         * @param value This parameter receives the value that the user has typed.
         * @return Indicates the outcome of the validation. This is sent to the browser.
         */
        public FormValidation doCheckUsername(@QueryParameter String value) {
            if (value.length() == 0)
                return FormValidation.error("Please set a username.");
            return FormValidation.ok();
        }

        /**
         * Validation of the 'profile' form Field.
         *
         * @param value This parameter receives the value that the user has typed.
         * @return Indicates the outcome of the validation. This is sent to the browser.
         */
        public FormValidation doCheckProfileName(@QueryParameter String value) {
            if (value.length() == 0)
                return FormValidation.error("Please set a profile name.");
            return FormValidation.ok();
        }

        /**
         * Validation of the 'apiKey' form Field.
         *
         * @param value This parameter receives the value that the user has typed.
         * @return Indicates the outcome of the validation. This is sent to the browser.
         */
        public FormValidation doCheckApiKey(@QueryParameter String value) {
            if (value.length() == 0)
                return FormValidation.error("Please set an API Key.");
            return FormValidation.ok();
        }

        /**
         * Validation of the 'serviceKey' form Field.
         *
         * @param value This parameter receives the value that the user has typed.
         * @return Indicates the outcome of the validation. This is sent to the browser.
         */
        public FormValidation doCheckServiceKey(@QueryParameter String value) {
            if (value.length() == 0)
                return FormValidation.error("Please set a Service Key.");
            return FormValidation.ok();
        }

        /**
         * Validation of the 'orgUuid' form Field.
         *
         * @param value This parameter receives the value that the user has typed.
         * @return Indicates the outcome of the validation. This is sent to the browser.
         */
        public FormValidation doCheckOrgUuid(@QueryParameter String value) {
            if (value.length() == 0)
                return FormValidation.error("Please set an Organization Uuid.");
            return FormValidation.ok();
        }

        /**
         * Validation of the 'teamServerUrl' form Field.
         *
         * @param value This parameter receives the value that the user has typed.
         * @return Indicates the outcome of the validation. This is sent to the browser.
         */
        public FormValidation doCheckTeamServerUrl(@QueryParameter String value) {
            if (value.length() == 0)
                return FormValidation.error("Please set a TeamServer Url.");
            return FormValidation.ok();
        }

        /**
         * Validation of the 'applicationName' form Field.
         *
         * @param value This parameter receives the value that the user has typed.
         * @return Indicates the outcome of the validation. This is sent to the browser.
         */
        public FormValidation doCheckApplicationName(@QueryParameter String value) {
            if (value.length() == 0)
                return FormValidation.error("Please set an Application Name.");
            return FormValidation.ok();
        }

        @Override
        public String getDisplayName() {
            return "Contrast Plugin Configuration";
        }
    }
}