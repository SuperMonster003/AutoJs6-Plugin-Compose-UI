"ui";

let picture = images.read(files.path('./sample.png'));

compose.mount(function () {
    return (
        <compose.Column padding={16}>
            <compose.Image key='picture' src={picture} w={160} h={120} contentScale='fit' contentDescription='Sample image' />
        </compose.Column>
    );
});
