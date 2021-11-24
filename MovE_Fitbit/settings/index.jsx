function mySettings(props) {
  return (
    <Page>
      <Section
        title={<Text bold align="center">Fitbit Account</Text>}>
        <Oauth
          settingsKey="oauth"
          title="Login"
          label="Fitbit"
          status="Login"
          authorizeUrl="https://www.fitbit.com/oauth2/authorize"
          requestTokenUrl="https://api.fitbit.com/oauth2/token"
          clientId="22BGDN"
          clientSecret="4e706b55fee4f592f88d2bc26ca7f3ae"
          scope="sleep"
        />
      </Section>
    </Page>
  );
}

registerSettingsPage(mySettings);