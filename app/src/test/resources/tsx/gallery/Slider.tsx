"ui";

let volume = compose.state(40);

compose.mount(function () {
    return (
        <compose.Column padding={16} spacing={8}>
            <compose.Text key='value' text={'Volume: ' + Math.round(volume.value)} />
            <compose.Slider key='volume' value={volume.value} range={[0, 100]} steps={9} onValueChange={function (value) { volume.value = value; }} />
        </compose.Column>
    );
});
